package com.pixelguard.applock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pixelguard.applock.AppLockApplication
import com.pixelguard.applock.data.local.PreferencesDataStore
import com.pixelguard.applock.domain.usecase.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            // Wipe all session tokens
            SessionManager.clearAllSessions()

            // PG-SEC-12: Use goAsync() so Android does not kill the process before async initialization completes
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dataStore = PreferencesDataStore(context)
                    val attempts = dataStore.globalFailedAttemptsFlow.first()
                    val wallClockExpiry = dataStore.globalLockoutWallClockTimestampFlow.first()
                    val durationMs = dataStore.globalLockoutDurationMsFlow.first()
                    SessionManager.syncPersistedLockout(
                        attempts = attempts,
                        wallClockExpiryMs = wallClockExpiry,
                        durationRemainingMs = durationMs
                    )

                    val preferences = AppLockApplication.instance.repository.preferences.first()
                    if (preferences.isForegroundServiceEnabled) {
                        val serviceIntent = Intent(context, AppLockForegroundService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
