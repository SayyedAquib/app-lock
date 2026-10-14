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
import com.pixelguard.applock.data.model.isNotificationMasked
import com.pixelguard.applock.domain.usecase.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationShieldService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName ?: return

        // Prevent self-interception or looping on our own masked notifications
        if (packageName == applicationContext.packageName) return

        scope.launch {
            try {
                val repository = AppLockApplication.instance.repository
                val preferences = repository.preferences.first()

                if (!preferences.isNotificationShieldEnabled) return@launch

                val lockedPackages = repository.lockedPackages.first()
                if (lockedPackages.contains(packageName) && preferences.isNotificationMasked(packageName)) {
                    val isUnlocked = SessionManager.isTemporarilyUnlocked(packageName, preferences.gracePeriodSeconds)
                    if (!isUnlocked) {
                        // 1. Cancel the exposed raw notification preview
                        cancelNotification(sbn.key)
                        if (com.pixelguard.applock.BuildConfig.DEBUG) {
                            Log.d(TAG, "Shielded notification preview for locked package: $packageName")
                        }

                        // 2. Post a safe masked placeholder so the user knows they have a message
                        postMaskedNotification(packageName, sbn.id)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling notification: ${e.message}")
            }
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
