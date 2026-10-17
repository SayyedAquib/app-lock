package com.pixelguard.applock.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.pixelguard.applock.data.local.CryptoManager
import com.pixelguard.applock.data.local.PreferencesDataStore
import com.pixelguard.applock.data.model.AppCategory
import com.pixelguard.applock.data.model.AppItem
import com.pixelguard.applock.data.model.IntruderRecord
import com.pixelguard.applock.data.model.LockPreferences
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class AppLockRepositoryImpl(
    private val context: Context,
    private val preferencesDataStore: PreferencesDataStore,
    private val cryptoManager: CryptoManager = CryptoManager()
) : AppLockRepository {

    override val lockedPackages: Flow<Set<String>> = preferencesDataStore.lockedPackagesFlow
    override val preferences: Flow<LockPreferences> = preferencesDataStore.preferencesFlow
    override val hasPinSetFlow: Flow<Boolean> = preferencesDataStore.pinHashFlow.map { it != null }

    private val _intruderRecords = MutableStateFlow<List<IntruderRecord>>(emptyList())
    override val intruderRecords: Flow<List<IntruderRecord>> = _intruderRecords.asStateFlow()

    init {
        loadIntruderRecords()
    }

    override suspend fun setAppLocked(packageName: String, locked: Boolean) {
        preferencesDataStore.togglePackageLock(packageName, locked)
        if (locked) {
            com.pixelguard.applock.domain.usecase.SessionManager.lockPackage(packageName)
        }
    }

    override suspend fun lockAllSensitiveApps(sensitivePackages: List<String>) {
        val current = lockedPackages.first().toMutableSet()
        current.addAll(sensitivePackages)
        preferencesDataStore.setLockedPackages(current)
        sensitivePackages.forEach { com.pixelguard.applock.domain.usecase.SessionManager.lockPackage(it) }
    }

    override suspend fun unlockPackages(packages: List<String>) {
        val current = lockedPackages.first().toMutableSet()
        current.removeAll(packages.toSet())
        preferencesDataStore.setLockedPackages(current)
    }

    override suspend fun unlockAllApps() {
        preferencesDataStore.setLockedPackages(emptySet())
        com.pixelguard.applock.domain.usecase.SessionManager.clearAllSessions()
    }

    override suspend fun updatePreferences(preferences: LockPreferences) {
        preferencesDataStore.updatePreferences(preferences)
    }

    override suspend fun hasPinSet(): Boolean {
        return preferencesDataStore.pinHashFlow.first() != null
    }

    override suspend fun setPin(newPin: String) {
        val salt = cryptoManager.generateSalt()
        val chars = newPin.toCharArray()
        val hash = cryptoManager.hashPin(chars, salt) // SEC-006: hashPin zeroes chars buffer
        val saltHex = salt.joinToString("") { "%02x".format(it) }
        preferencesDataStore.savePin(hash, saltHex)
    }

    override suspend fun verifyPin(inputPin: String): Boolean {
        val storedHash = preferencesDataStore.pinHashFlow.first()
        val storedSaltHex = preferencesDataStore.pinSaltFlow.first()
        if (storedHash == null || storedSaltHex == null) {
            // No PIN has been configured yet — deny access until user sets one via Onboarding.
            return false
        }
        val salt = storedSaltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val chars = inputPin.toCharArray()
        val computedHash = cryptoManager.hashPin(chars, salt) // SEC-006: hashPin zeroes chars buffer
        return cryptoManager.verifyHash(computedHash, storedHash)
    }

    override suspend fun getInstalledApps(): List<AppItem> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList = packageManager.queryIntentActivities(mainIntent, 0)
        val currentLocked = lockedPackages.first()

        val apps = mutableListOf<AppItem>()
        for (resolveInfo in resolveInfoList) {
            val pkgName = resolveInfo.activityInfo.packageName
            if (pkgName == context.packageName) continue // Exclude self

            val appName = resolveInfo.loadLabel(packageManager).toString()
            val icon = resolveInfo.loadIcon(packageManager)
            val iconBitmap = try {
                icon?.toBitmap(width = 120, height = 120, config = Bitmap.Config.ARGB_8888)
            } catch (_: Exception) {
                null
            }
            val category = categorizePackage(pkgName, resolveInfo.activityInfo.applicationInfo)
            val isLocked = currentLocked.contains(pkgName)

            apps.add(AppItem(pkgName, appName, icon, iconBitmap, category, isLocked))
        }

        // Add Android Settings app specifically if not included in launcher list
        val settingsPkg = "com.android.settings"
        if (apps.none { it.packageName == settingsPkg }) {
            try {
                val appInfo = packageManager.getApplicationInfo(settingsPkg, 0)
                val label = packageManager.getApplicationLabel(appInfo).toString()
                val icon = packageManager.getApplicationIcon(appInfo)
                val settingsBitmap = try {
                    icon?.toBitmap(width = 120, height = 120, config = Bitmap.Config.ARGB_8888)
                } catch (_: Exception) {
                    null
                }
                apps.add(AppItem(settingsPkg, label, icon, settingsBitmap, AppCategory.SYSTEM, currentLocked.contains(settingsPkg)))
            } catch (_: Exception) {}
        }

        apps.sortedWith(compareBy({ !it.isLocked }, { it.appName.lowercase() }))
    }

    private fun categorizePackage(packageName: String, appInfo: ApplicationInfo): AppCategory {
        val lower = packageName.lowercase()
        return when {
            // Social / Messaging
            lower.contains("whatsapp") || lower.contains("instagram") ||
            lower.contains("telegram") || lower.contains("snapchat") ||
            lower.contains("facebook") || lower.contains("twitter") ||
            lower.contains("tiktok") || lower.contains("messenger") ||
            lower.contains("discord") || lower.contains("reddit") -> AppCategory.SOCIAL

            // Financial / Banking
            lower.contains("bank") || lower.contains("pay") ||
            lower.contains("paisa") || lower.contains("phonepe") ||
            lower.contains("upi") || lower.contains("bhim") ||
            lower.contains("wallet") || lower.contains("crypto") ||
            lower.contains("binance") || lower.contains("coinbase") ||
            lower.contains("chase") || lower.contains("revolut") -> AppCategory.FINANCIAL

            // System / Settings / Store
            packageName == "com.android.settings" ||
            packageName == "com.android.vending" ||
            packageName == "com.google.android.packageinstaller" ||
            packageName == "com.android.packageinstaller" ||
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 -> AppCategory.SYSTEM

            // High Sensitivity (Photos, Gallery, Files, Mail)
            lower.contains("gallery") || lower.contains("photo") ||
            lower.contains("camera") || lower.contains("file") ||
            lower.contains("drive") || lower.contains("mail") ||
            lower.contains("gmail") || lower.contains("notes") -> AppCategory.SENSITIVE

            else -> AppCategory.OTHER
        }
    }

    private fun parseRecordsJson(jsonStr: String): List<IntruderRecord> {
        val array = JSONArray(jsonStr)
        val list = mutableListOf<IntruderRecord>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                IntruderRecord(
                    id = obj.getString("id"),
                    timestamp = obj.getLong("timestamp"),
                    targetPackageName = obj.getString("targetPackageName"),
                    targetAppName = obj.getString("targetAppName"),
                    photoPath = obj.getString("photoPath")
                )
            )
        }
        return list.sortedByDescending { it.timestamp }
    }

    private fun loadIntruderRecords() {
        val encFile = File(context.filesDir, "intruder_records.enc")
        val legacyJsonFile = File(context.filesDir, "intruder_records.json")

        // 1. SEC-005: Read encrypted intruder metadata
        if (encFile.exists()) {
            try {
                val encBytes = encFile.readBytes()
                val decBytes = cryptoManager.decryptFromFileBytes(encBytes)
                val jsonStr = String(decBytes, Charsets.UTF_8)
                _intruderRecords.value = parseRecordsJson(jsonStr)
                return
            } catch (_: Exception) {
                _intruderRecords.value = emptyList()
            }
        }

        // 2. Seamless Auto-Migration: If legacy plaintext JSON exists, auto-encrypt and delete legacy file
        if (legacyJsonFile.exists()) {
            try {
                val jsonStr = legacyJsonFile.readText()
                val list = parseRecordsJson(jsonStr)
                _intruderRecords.value = list
                saveIntruderRecords(list) // Writes encrypted intruder_records.enc
                legacyJsonFile.delete() // Securely remove legacy unencrypted file
            } catch (_: Exception) {
                _intruderRecords.value = emptyList()
            }
        } else {
            _intruderRecords.value = emptyList()
        }
    }

    private fun saveIntruderRecords(records: List<IntruderRecord>) {
        val encFile = File(context.filesDir, "intruder_records.enc")
        val array = JSONArray()
        records.forEach { record ->
            val obj = JSONObject().apply {
                put("id", record.id)
                put("timestamp", record.timestamp)
                put("targetPackageName", record.targetPackageName)
                put("targetAppName", record.targetAppName)
                put("photoPath", record.photoPath)
            }
            array.put(obj)
        }
        try {
            val jsonBytes = array.toString().toByteArray(Charsets.UTF_8)
            val encrypted = cryptoManager.encryptToFileBytes(jsonBytes)
            encFile.writeBytes(encrypted)
        } catch (_: Exception) {}
    }

    override suspend fun addIntruderRecord(record: IntruderRecord) {
        val updated = _intruderRecords.value.toMutableList()
        updated.add(0, record)
        _intruderRecords.value = updated
        saveIntruderRecords(updated)
    }

    override suspend fun deleteIntruderRecord(id: String) {
        val record = _intruderRecords.value.find { it.id == id }
        record?.let {
            val photoFile = File(it.photoPath)
            if (photoFile.exists()) photoFile.delete()
        }
        val updated = _intruderRecords.value.filterNot { it.id == id }
        _intruderRecords.value = updated
        saveIntruderRecords(updated)
    }

    override suspend fun clearAllIntruderRecords() {
        _intruderRecords.value.forEach {
            val photoFile = File(it.photoPath)
            if (photoFile.exists()) photoFile.delete()
        }
        _intruderRecords.value = emptyList()
        val encFile = File(context.filesDir, "intruder_records.enc")
        if (encFile.exists()) encFile.delete()
        val legacyFile = File(context.filesDir, "intruder_records.json")
        if (legacyFile.exists()) legacyFile.delete()
    }

    override suspend fun enableRecommendedShields() {
        val current = preferences.first()
        updatePreferences(current.copy(isSettingsProtected = true, isRecentsMaskingEnabled = true))
    }
}
