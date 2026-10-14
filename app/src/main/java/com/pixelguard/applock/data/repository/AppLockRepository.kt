package com.pixelguard.applock.data.repository

import com.pixelguard.applock.data.model.AppItem
import com.pixelguard.applock.data.model.IntruderRecord
import com.pixelguard.applock.data.model.LockPreferences
import kotlinx.coroutines.flow.Flow

interface AppLockRepository {
    val lockedPackages: Flow<Set<String>>
    val preferences: Flow<LockPreferences>
    val intruderRecords: Flow<List<IntruderRecord>>
    val hasPinSetFlow: Flow<Boolean>

    suspend fun setAppLocked(packageName: String, locked: Boolean)
    suspend fun lockAllSensitiveApps(sensitivePackages: List<String>)
    suspend fun unlockPackages(packages: List<String>)
    suspend fun unlockAllApps()
    suspend fun updatePreferences(preferences: LockPreferences)
    suspend fun hasPinSet(): Boolean
    suspend fun setPin(newPin: String)
    suspend fun verifyPin(inputPin: String): Boolean
    suspend fun getInstalledApps(): List<AppItem>
    suspend fun addIntruderRecord(record: IntruderRecord)
    suspend fun deleteIntruderRecord(id: String)
    suspend fun clearAllIntruderRecords()
}
