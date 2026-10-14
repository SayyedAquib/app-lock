package com.pixelguard.applock.ui.main

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelguard.applock.AppLockApplication
import com.pixelguard.applock.data.model.AppCategory
import com.pixelguard.applock.data.model.AppItem
import com.pixelguard.applock.data.model.AppSortOrder
import com.pixelguard.applock.data.model.IntruderRecord
import com.pixelguard.applock.data.model.LockPreferences
import com.pixelguard.applock.data.repository.AppLockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PermissionStatus(
    val hasOverlayPermission: Boolean,
    val hasAccessibilityPermission: Boolean,
    val hasNotificationPermission: Boolean
) {
    val isAllGranted: Boolean
        get() = hasOverlayPermission && hasAccessibilityPermission
}

class MainViewModel(
    private val repository: AppLockRepository = AppLockApplication.instance.repository
) : ViewModel() {

    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val allApps: StateFlow<List<AppItem>> = combine(_installedApps, repository.lockedPackages) { apps, locked ->
        apps.map { it.copy(isLocked = locked.contains(it.packageName)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val installedApps: StateFlow<List<AppItem>> = allApps

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<AppCategory?>(null)
    val selectedCategory: StateFlow<AppCategory?> = _selectedCategory.asStateFlow()

    private val _isOnlyLockedFilterActive = MutableStateFlow(false)
    val isOnlyLockedFilterActive: StateFlow<Boolean> = _isOnlyLockedFilterActive.asStateFlow()

    private val _permissionStatus = MutableStateFlow(
        PermissionStatus(hasOverlayPermission = false, hasAccessibilityPermission = false, hasNotificationPermission = false)
    )
    val permissionStatus: StateFlow<PermissionStatus> = _permissionStatus.asStateFlow()

    val preferences: StateFlow<LockPreferences> = repository.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, LockPreferences())

    val intruderRecords: StateFlow<List<IntruderRecord>> = repository.intruderRecords
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val hasPinSet: StateFlow<Boolean?> = repository.hasPinSetFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val filteredApps: StateFlow<List<AppItem>> = combine(
        allApps,
        _searchQuery,
        _selectedCategory,
        _isOnlyLockedFilterActive,
        preferences
    ) { appsWithLock, query, category, onlyLocked, prefs ->
        appsWithLock
            .filter { app ->
                val matchesQuery = query.isBlank() || app.appName.contains(query, ignoreCase = true)
                val matchesCategory = category == null || app.category == category
                val matchesLocked = !onlyLocked || app.isLocked
                matchesQuery && matchesCategory && matchesLocked
            }
            .let { list ->
                when (prefs.appSortOrder) {
                    AppSortOrder.NAME_ASC -> list.sortedBy { it.appName.lowercase() }
                    AppSortOrder.LOCKED_FIRST -> list.sortedWith(
                        compareByDescending<AppItem> { it.isLocked }
                            .thenBy { it.appName.lowercase() }
                    )
                    AppSortOrder.CATEGORY -> list.sortedWith(
                        compareBy<AppItem> { it.category.ordinal }
                            .thenBy { it.appName.lowercase() }
                    )
                }
            }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _installedApps.value = repository.getInstalledApps()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: AppCategory?) {
        _selectedCategory.value = category
    }

    fun toggleOnlyLockedFilter() {
        _isOnlyLockedFilterActive.value = !_isOnlyLockedFilterActive.value
    }

    fun setSortOrder(order: AppSortOrder) {
        viewModelScope.launch {
            repository.updatePreferences(preferences.value.copy(appSortOrder = order))
        }
    }

    fun resetFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = null
        _isOnlyLockedFilterActive.value = false
    }

    fun toggleAppLock(packageName: String, isCurrentlyLocked: Boolean) {
        viewModelScope.launch {
            repository.setAppLocked(packageName, !isCurrentlyLocked)
        }
    }

    fun protectAllSensitiveApps() {
        viewModelScope.launch {
            val sensitivePackages = _installedApps.value
                .filter { it.category == AppCategory.SENSITIVE || it.category == AppCategory.FINANCIAL || it.category == AppCategory.SOCIAL }
                .map { it.packageName }
            repository.lockAllSensitiveApps(sensitivePackages)
        }
    }

    fun lockCategory(category: AppCategory) {
        viewModelScope.launch {
            val packages = _installedApps.value
                .filter { it.category == category }
                .map { it.packageName }
            repository.lockAllSensitiveApps(packages)
        }
    }

    fun unlockCategory(category: AppCategory) {
        viewModelScope.launch {
            val packages = _installedApps.value
                .filter { it.category == category }
                .map { it.packageName }
            repository.unlockPackages(packages)
        }
    }

    fun unlockAllApps() {
        viewModelScope.launch {
            repository.unlockAllApps()
        }
    }

    fun setMasterPin(pin: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.setPin(pin)
            onComplete()
        }
    }

    fun updatePreferences(newPrefs: LockPreferences) {
        viewModelScope.launch {
            repository.updatePreferences(newPrefs)
        }
    }

    fun setAppGracePeriod(packageName: String, seconds: Int) {
        viewModelScope.launch {
            val currentPrefs = preferences.value
            val json = try {
                org.json.JSONObject(currentPrefs.appGracePeriodsJson)
            } catch (_: Exception) {
                org.json.JSONObject()
            }
            if (seconds < 0) {
                json.remove(packageName)
            } else {
                json.put(packageName, seconds)
            }
            repository.updatePreferences(currentPrefs.copy(appGracePeriodsJson = json.toString()))
        }
    }

    fun togglePinOnly(packageName: String, requirePinOnly: Boolean) {
        viewModelScope.launch {
            val currentPrefs = preferences.value
            val set = mutableSetOf<String>()
            try {
                val array = org.json.JSONArray(currentPrefs.pinOnlyPackagesJson)
                for (i in 0 until array.length()) {
                    val pkg = array.optString(i)
                    if (pkg.isNotBlank()) set.add(pkg)
                }
            } catch (_: Exception) {}

            if (requirePinOnly) {
                set.add(packageName)
            } else {
                set.remove(packageName)
            }
            val newArray = org.json.JSONArray(set.toList())
            repository.updatePreferences(currentPrefs.copy(pinOnlyPackagesJson = newArray.toString()))
        }
    }

    fun toggleNotificationMasking(packageName: String, isMasked: Boolean) {
        viewModelScope.launch {
            val currentPrefs = preferences.value
            val exemptSet = mutableSetOf<String>()
            try {
                val array = org.json.JSONArray(currentPrefs.notificationExemptPackagesJson)
                for (i in 0 until array.length()) {
                    val pkg = array.optString(i)
                    if (pkg.isNotBlank()) exemptSet.add(pkg)
                }
            } catch (_: Exception) {}

            // Opt-out architecture: notificationExemptPackagesJson contains packages that should NOT be masked
            if (isMasked) {
                exemptSet.remove(packageName)
            } else {
                exemptSet.add(packageName)
            }
            val newArray = org.json.JSONArray(exemptSet.toList())
            repository.updatePreferences(currentPrefs.copy(notificationExemptPackagesJson = newArray.toString()))
        }
    }

    fun deleteIntruderRecord(id: String) {
        viewModelScope.launch {
            repository.deleteIntruderRecord(id)
        }
    }

    fun clearAllIntruders() {
        viewModelScope.launch {
            repository.clearAllIntruderRecords()
        }
    }

    @androidx.annotation.VisibleForTesting
    var isTestingPermissions: Boolean = false

    fun checkPermissions(context: Context) {
        if (isTestingPermissions) return
        val hasOverlay = Settings.canDrawOverlays(context)
        val accessibilityEnabled = isAccessibilityServiceEnabled(context)
        val notificationEnabled = isNotificationServiceEnabled(context)

        _permissionStatus.value = PermissionStatus(
            hasOverlayPermission = hasOverlay,
            hasAccessibilityPermission = accessibilityEnabled,
            hasNotificationPermission = notificationEnabled
        )
    }

    @androidx.annotation.VisibleForTesting
    fun setPermissionStatusForTest(status: PermissionStatus) {
        isTestingPermissions = true
        _permissionStatus.value = status
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedService = "${context.packageName}/${com.pixelguard.applock.service.AppLockAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(expectedService)
    }

    private fun isNotificationServiceEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
        return flat.contains(context.packageName)
    }
}
