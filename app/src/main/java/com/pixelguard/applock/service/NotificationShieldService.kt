package com.pixelguard.applock.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pixelguard.applock.AppLockApplication
import com.pixelguard.applock.R
import com.pixelguard.applock.data.model.LockPreferences
import com.pixelguard.applock.data.model.isNotificationMasked
import com.pixelguard.applock.domain.usecase.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationShieldService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    @Volatile private var cachedPreferences: LockPreferences? = null
    @Volatile private var cachedLockedPackages: Set<String> = emptySet()

    override fun onCreate() {
        super.onCreate()
        startObservingDataStore()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        startObservingDataStore()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun startObservingDataStore() {
        scope.launch(Dispatchers.IO) {
            try {
                AppLockApplication.instance.repository.preferences.collect { prefs ->
                    cachedPreferences = prefs
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting preferences: ${e.message}")
            }
        }
        scope.launch(Dispatchers.IO) {
            try {
                AppLockApplication.instance.repository.lockedPackages.collect { pkgs ->
                    cachedLockedPackages = pkgs
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting lockedPackages: ${e.message}")
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        // PG-SEC-10: Exempt ongoing system notifications (active calls, alarms, stopwatch, media players)
        if (sbn.isOngoing) return

        val packageName = sbn.packageName ?: return

        // Prevent self-interception or looping on our own masked notifications
        if (packageName == applicationContext.packageName) return

        val preferences = cachedPreferences
        val lockedPackages = cachedLockedPackages

        // Fast-path: If cache is populated, check conditions immediately in memory
        if (preferences != null) {
            if (!preferences.isNotificationShieldEnabled) return
            if (!lockedPackages.contains(packageName)) return
            if (!preferences.isNotificationMasked(packageName)) return
            handleNotificationInterception(sbn, packageName, preferences)
        } else {
            // Cold-start fallback: query DataStore asynchronously
            scope.launch {
                try {
                    val repository = AppLockApplication.instance.repository
                    val currentPrefs = repository.preferences.first()
                    cachedPreferences = currentPrefs
                    val currentLocked = repository.lockedPackages.first()
                    cachedLockedPackages = currentLocked

                    if (!currentPrefs.isNotificationShieldEnabled) return@launch
                    if (currentLocked.contains(packageName) && currentPrefs.isNotificationMasked(packageName)) {
                        handleNotificationInterception(sbn, packageName, currentPrefs)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling notification in fallback: ${e.message}")
                }
            }
        }
    }

    private fun handleNotificationInterception(
        sbn: StatusBarNotification,
        packageName: String,
        preferences: LockPreferences
    ) {
        val isNight = SessionManager.isNightLockdownActive(
            preferences.isScheduleEnabled,
            preferences.scheduleStartHour,
            preferences.scheduleEndHour
        )
        val effectiveGrace = SessionManager.getEffectiveGracePeriod(
            packageName = packageName,
            defaultGraceSeconds = preferences.gracePeriodSeconds,
            isNightLockdown = isNight,
            isPerAppGraceEnabled = preferences.isPerAppGraceEnabled,
            appGracePeriodsJson = preferences.appGracePeriodsJson
        )
        val isUnlocked = SessionManager.isTemporarilyUnlocked(packageName, effectiveGrace)
        if (!isUnlocked) {
            // 1. Immediately cancel raw notification to prevent preview flash in status bar
            cancelNotification(sbn.key)
            if (com.pixelguard.applock.BuildConfig.DEBUG) {
                Log.d(TAG, "Shielded notification preview for locked package: $packageName")
            }

            // 2. Post safe masked placeholder so user knows they have a message
            postMaskedNotification(packageName, sbn.id)
        }
    }

    private fun postMaskedNotification(packageName: String, notificationId: Int) {
        val packageManager = packageManager
        val appName = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            "Locked App"
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: Intent()
        val pendingIntent = PendingIntent.getActivity(
            this,
            packageName.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, AppLockApplication.CHANNEL_ID_MASKED)
            .setSmallIcon(R.drawable.ic_lock_status)
            .setContentTitle(appName)
            .setContentText("New notification • Content hidden by Pixel Guard")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(packageName, notificationId, notification)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No-op
    }

    companion object {
        private const val TAG = "NotificationShield"
    }
}
