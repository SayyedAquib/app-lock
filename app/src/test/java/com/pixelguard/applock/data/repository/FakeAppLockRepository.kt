package com.pixelguard.applock.data.repository

import com.pixelguard.applock.data.model.AppItem
import com.pixelguard.applock.data.model.IntruderRecord
import com.pixelguard.applock.data.model.LockPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An in-memory, deterministic fake implementation of [AppLockRepository] for unit tests.
 * Enables fast JVM testing of ViewModels, UseCases, and StateFlow pipelines without DataStore or SQLite.
 */
class FakeAppLockRepository(
    initialLockedPackages: Set<String> = emptySet(),
    initialPreferences: LockPreferences = LockPreferences(),
    initialHasPin: Boolean = false,
    initialApps: List<AppItem> = emptyList()
) : AppLockRepository {

    private val _lockedPackages = MutableStateFlow(initialLockedPackages)
    override val lockedPackages: Flow<Set<String>> = _lockedPackages.asStateFlow()

    private val _preferences = MutableStateFlow(initialPreferences)
    override val preferences: Flow<LockPreferences> = _preferences.asStateFlow()

    private val _intruderRecords = MutableStateFlow<List<IntruderRecord>>(emptyList())
    override val intruderRecords: Flow<List<IntruderRecord>> = _intruderRecords.asStateFlow()

    fun setIntruderRecords(records: List<IntruderRecord>) {
        _intruderRecords.value = records
    }

    private val _hasPinSetFlow = MutableStateFlow(initialHasPin)
    override val hasPinSetFlow: Flow<Boolean> = _hasPinSetFlow.asStateFlow()

    var installedAppsList: List<AppItem> = initialApps
    var storedPin: String? = null

    override suspend fun setAppLocked(packageName: String, locked: Boolean) {
        val current = _lockedPackages.value.toMutableSet()
        if (locked) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        _lockedPackages.value = current
    }

    override suspend fun lockAllSensitiveApps(sensitivePackages: List<String>) {
        _lockedPackages.value = _lockedPackages.value + sensitivePackages.toSet()
    }

    override suspend fun unlockPackages(packages: List<String>) {
        _lockedPackages.value = _lockedPackages.value - packages.toSet()
    }

    override suspend fun unlockAllApps() {
        _lockedPackages.value = emptySet()
    }

    override suspend fun updatePreferences(preferences: LockPreferences) {
        _preferences.value = preferences
    }

    override suspend fun hasPinSet(): Boolean {
        return _hasPinSetFlow.value
    }

    override suspend fun setPin(newPin: String) {
        storedPin = newPin
        _hasPinSetFlow.value = true
    }

    override suspend fun verifyPin(inputPin: String): Boolean {
        return storedPin != null && storedPin == inputPin
    }

    override suspend fun getInstalledApps(): List<AppItem> {
        return installedAppsList
    }

    override suspend fun addIntruderRecord(record: IntruderRecord) {
        _intruderRecords.value = listOf(record) + _intruderRecords.value
    }

    override suspend fun deleteIntruderRecord(id: String) {
        _intruderRecords.value = _intruderRecords.value.filterNot { it.id == id }
    }

    override suspend fun clearAllIntruderRecords() {
        _intruderRecords.value = emptyList()
    }

    override suspend fun enableRecommendedShields() {
        val current = _preferences.value
        _preferences.value = current.copy(isSettingsProtected = true, isRecentsMaskingEnabled = true)
    }
}
