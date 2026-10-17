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

            // Restore persistent brute-force lockout state across reboots
            CoroutineScope(Dispatchers.IO).launch {
                val dataStore = PreferencesDataStore(context)
                val attempts = dataStore.globalFailedAttemptsFlow.first()
                val lockoutUntil = dataStore.globalLockoutUntilTimestampFlow.first()
                SessionManager.syncPersistedLockout(attempts, lockoutUntil)

                val preferences = AppLockApplication.instance.repository.preferences.first()
                if (preferences.isForegroundServiceEnabled) {
                    val serviceIntent = Intent(context, AppLockForegroundService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            }
        }
    }
}
