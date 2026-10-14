package com.pixelguard.applock.data.model

enum class LockType {
    PIN,
    PATTERN
}

enum class AppSortOrder {
    NAME_ASC,
    LOCKED_FIRST,
    CATEGORY
}

data class LockPreferences(
    val isBiometricsEnabled: Boolean = true,
    val lockType: LockType = LockType.PIN,
    val gracePeriodSeconds: Int = 0, // 0 = Immediately on app exit/screen off
    val isIntruderSelfieEnabled: Boolean = true,
    val failedAttemptsThreshold: Int = 3,
    val isDecoyCrashEnabled: Boolean = false,
    val isNotificationShieldEnabled: Boolean = true,
    val isSettingsProtected: Boolean = false,
    val isPlayStoreProtected: Boolean = false,
    val isScramblePinEnabled: Boolean = false,
    val isForegroundServiceEnabled: Boolean = false,

    // 1. Recents Privacy Masking (FLAG_SECURE)
    val isRecentsMaskingEnabled: Boolean = false,

    // 2. Quick Settings Tile
    val isQuickTileEnabled: Boolean = true,
    val quickTilePauseMinutes: Int = 15, // 5, 15, 30, 60, or -1 (Until resumed)

    // 3. Pixel Haptic Feedback
    val isHapticsEnabled: Boolean = true,

    // 4. Fluid Micro-Animations
    val isAnimationsEnabled: Boolean = true,

    // 5. Smart Schedule / Night Lockdown Mode
    val isScheduleEnabled: Boolean = false,
    val scheduleStartHour: Int = 23, // 11 PM
    val scheduleEndHour: Int = 7,    // 7 AM
    val isNightLockdownPinOnly: Boolean = false, // Require Master PIN only at night (disables biometrics)

    // 6. Custom Per-App Grace Periods
    val isPerAppGraceEnabled: Boolean = true,
    val appGracePeriodsJson: String = "{}",

    // 7. Dynamic App-Themed Material You Lock Screen
    val isDynamicAppThemingEnabled: Boolean = true,

    // 8. Dashboard App Sorting
    val appSortOrder: AppSortOrder = AppSortOrder.NAME_ASC,

    // 9. Per-App Security Profile overrides
    val pinOnlyPackagesJson: String = "[]",
    val notificationExemptPackagesJson: String = "[]"
)

fun LockPreferences.isPinOnly(packageName: String): Boolean {
    if (pinOnlyPackagesJson.isBlank() || pinOnlyPackagesJson == "[]") return false
    return try {
        val array = org.json.JSONArray(pinOnlyPackagesJson)
        for (i in 0 until array.length()) {
            if (array.optString(i) == packageName) return true
        }
        false
    } catch (_: Exception) {
        pinOnlyPackagesJson.contains("\"$packageName\"")
    }
}

fun LockPreferences.isNotificationMasked(packageName: String): Boolean {
    if (!isNotificationShieldEnabled) return false
    if (notificationExemptPackagesJson.isBlank() || notificationExemptPackagesJson == "[]") return true
    return try {
        val array = org.json.JSONArray(notificationExemptPackagesJson)
        for (i in 0 until array.length()) {
            if (array.optString(i) == packageName) return false
        }
        true
    } catch (_: Exception) {
        !notificationExemptPackagesJson.contains("\"$packageName\"")
    }
}
