package com.pixelguard.applock.domain.usecase

import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

object SessionManager {

    // Maps packageName -> unlock timestamp (in milliseconds)
    private val unlockedSessions = ConcurrentHashMap<String, Long>()

    // Maps packageName -> exit timestamp (in milliseconds) when the user switched away
    private val exitTimestamps = ConcurrentHashMap<String, Long>()

    // Per-package failed attempt counters (persist across LockActivity recreations)
    private val failedAttempts = ConcurrentHashMap<String, Int>()

    // Per-package lockout timestamp (in milliseconds) for brute-force rate limiting
    private val lockoutTimestamps = ConcurrentHashMap<String, Long>()

    // Monotonic Clock Provider (SEC-002):
    // android.os.SystemClock.elapsedRealtime() is strictly monotonic and immune to user clock adjustments.
    // Falls back to System.currentTimeMillis() if unmocked (0L) on pure JVM unit tests.
    private var timeProvider: () -> Long = {
        try {
            val monotonic = android.os.SystemClock.elapsedRealtime()
            if (monotonic > 0L) monotonic else System.currentTimeMillis()
        } catch (_: Throwable) {
            System.currentTimeMillis()
        }
    }

    fun now(): Long = timeProvider()

    internal fun setTimeProviderForTest(provider: () -> Long) {
        timeProvider = provider
    }

    // Current package currently being viewed/unlocked
    @Volatile
    var currentForegroundPackage: String? = null

    // Pause protection state (e.g. from Quick Settings Tile)
    @Volatile
    private var pauseUntilTimestamp: Long = 0L

    fun pauseProtection(durationMinutes: Int) {
        pauseUntilTimestamp = if (durationMinutes < 0) {
            Long.MAX_VALUE
        } else {
            now() + (durationMinutes * 60 * 1000L)
        }
    }

    internal fun pauseProtectionUntilForTest(timestampMs: Long) {
        pauseUntilTimestamp = timestampMs
    }

    fun resumeProtection() {
        pauseUntilTimestamp = 0L
    }

    fun isPaused(): Boolean {
        return now() < pauseUntilTimestamp
    }

    fun getRemainingPauseMinutes(): Int {
        if (pauseUntilTimestamp == Long.MAX_VALUE) return -1
        val diff = pauseUntilTimestamp - now()
        return if (diff > 0) ((diff / 60000) + 1).toInt() else 0
    }

    fun isNightLockdownActive(
        scheduleEnabled: Boolean,
        startHour: Int,
        endHour: Int,
        currentHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    ): Boolean {
        if (!scheduleEnabled) return false
        return if (startHour > endHour) {
            currentHour >= startHour || currentHour < endHour
        } else {
            currentHour in startHour until endHour
        }
    }

    fun getEffectiveGracePeriod(
        packageName: String,
        defaultGraceSeconds: Int,
        isNightLockdown: Boolean,
        isPerAppGraceEnabled: Boolean,
        appGracePeriodsJson: String
    ): Int {
        if (isNightLockdown) return 0 // Maximum lockdown at night

        if (!isPerAppGraceEnabled) return defaultGraceSeconds

        return try {
            val json = JSONObject(appGracePeriodsJson)
            if (json.has(packageName)) json.getInt(packageName) else defaultGraceSeconds
        } catch (_: Exception) {
            defaultGraceSeconds
        }
    }

    fun isTemporarilyUnlocked(packageName: String, effectiveGraceSeconds: Int): Boolean {
        if (isPaused()) return true // Protection paused via Quick Settings tile

        val unlockTime = unlockedSessions[packageName] ?: return false

        // 1. If currently in foreground, the app is actively in use — NEVER relock while using!
        if (currentForegroundPackage == packageName) {
            return true
        }

        val nowTime = now()

        // 2. Initial launch transition buffer (3s) to prevent flicker/race conditions
        if (nowTime - unlockTime < 3000L) {
            return true
        }

        // 3. User has navigated away from the app
        if (effectiveGraceSeconds <= 0) {
            // Immediate relock on exit
            return false
        }

        // 4. Grace period counts down from when the user exited the app
        val exitTime = exitTimestamps[packageName] ?: unlockTime
        val elapsedSinceExit = (nowTime - exitTime) / 1000
        return elapsedSinceExit < effectiveGraceSeconds
    }

    fun isRecentlyUnlocked(packageName: String, thresholdMs: Long = 3000L): Boolean {
        val unlockTime = unlockedSessions[packageName] ?: return false
        val diff = now() - unlockTime
        return diff in 0 until thresholdMs
    }

    fun markAppUnlocked(packageName: String) {
        val nowTime = now()
        unlockedSessions[packageName] = nowTime
        exitTimestamps.remove(packageName)
        currentForegroundPackage = packageName
    }

    fun markAppResumed(packageName: String) {
        currentForegroundPackage = packageName
        exitTimestamps.remove(packageName)
    }

    internal fun markAppUnlockedForTest(
        packageName: String,
        timestampMs: Long,
        inForeground: Boolean = true
    ) {
        unlockedSessions[packageName] = timestampMs
        if (inForeground) {
            currentForegroundPackage = packageName
            exitTimestamps.remove(packageName)
        } else {
            currentForegroundPackage = null
            exitTimestamps[packageName] = timestampMs
        }
    }

    internal fun markAppExitedForTest(packageName: String, exitTimestampMs: Long) {
        currentForegroundPackage = null
        exitTimestamps[packageName] = exitTimestampMs
    }

    fun onAppSwitched(newPackageName: String, effectiveGraceSeconds: Int) {
        if (currentForegroundPackage != newPackageName) {
            currentForegroundPackage?.let { prevPkg ->
                if (unlockedSessions.containsKey(prevPkg)) {
                    val nowTime = now()
                    exitTimestamps[prevPkg] = nowTime
                    // Only evict session immediately if 0s grace AND not in 3s launch buffer
                    if (effectiveGraceSeconds <= 0 && !isRecentlyUnlocked(prevPkg, 3000L)) {
                        unlockedSessions.remove(prevPkg)
                    }
                }
            }
            currentForegroundPackage = newPackageName
        }
    }

    fun onScreenOff() {
        val nowTime = now()
        currentForegroundPackage?.let { fgPkg ->
            exitTimestamps[fgPkg] = nowTime
            unlockedSessions.remove(fgPkg)
        }
        currentForegroundPackage = null
    }

    fun clearAllSessions() {
        unlockedSessions.clear()
        exitTimestamps.clear()
        currentForegroundPackage = null
        failedAttempts.clear()
        lockoutTimestamps.clear()
    }

    fun lockPackage(packageName: String) {
        unlockedSessions.remove(packageName)
        exitTimestamps.remove(packageName)
        if (currentForegroundPackage == packageName) {
            currentForegroundPackage = null
        }
    }

    // --- Failed Attempts & Rate Limiting API (SEC-002 Monotonic Clock) ---

    fun incrementFailedAttempts(packageName: String): Int {
        val count = (failedAttempts[packageName] ?: 0) + 1
        failedAttempts[packageName] = count
        val nowTime = now()
        if (count == 5) {
            lockoutTimestamps[packageName] = nowTime + 30_000L
        } else if (count >= 8) {
            lockoutTimestamps[packageName] = nowTime + 60_000L
        }
        return count
    }

    fun getFailedAttempts(packageName: String): Int {
        return failedAttempts[packageName] ?: 0
    }

    fun getRemainingLockoutSeconds(packageName: String): Int {
        val lockoutUntil = lockoutTimestamps[packageName] ?: return 0
        val remainingMs = lockoutUntil - now()
        return if (remainingMs > 0) ((remainingMs + 999) / 1000).toInt() else 0
    }

    fun isLockedOut(packageName: String): Boolean {
        return getRemainingLockoutSeconds(packageName) > 0
    }

    fun getAttemptsUntilLockout(packageName: String): Int {
        val current = failedAttempts[packageName] ?: 0
        return if (current < 5) 5 - current else 0
    }

    fun resetFailedAttempts(packageName: String) {
        failedAttempts.remove(packageName)
        lockoutTimestamps.remove(packageName)
    }

    internal fun setLockoutForTest(packageName: String, durationMs: Long) {
        lockoutTimestamps[packageName] = now() + durationMs
    }
}
