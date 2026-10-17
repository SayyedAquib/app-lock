package com.pixelguard.applock.service.overlay

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.pixelguard.applock.AppLockApplication
import com.pixelguard.applock.data.model.IntruderRecord
import com.pixelguard.applock.data.model.isPinOnly
import com.pixelguard.applock.domain.usecase.SessionManager
import com.pixelguard.applock.service.camera.SilentCameraCapture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class OverlayWindowManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val scope = CoroutineScope(Dispatchers.Main)
    private var currentOverlayView: ComposeView? = null
    private var currentLifecycleOwner: OverlayLifecycleOwner? = null
    private var activePackageName: String? = null

    fun isOverlayShowing(): Boolean = currentOverlayView != null

    fun showLockOverlay(packageName: String) {
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Cannot show overlay: SYSTEM_ALERT_WINDOW permission missing")
            return
        }

        if (currentOverlayView != null && activePackageName == packageName) {
            // Already showing for this package
            return
        }

        dismissOverlay()

        activePackageName = packageName

        val repository = AppLockApplication.instance.repository

        scope.launch {
            val preferences = repository.preferences.first()
            val packageManager = context.packageManager
            val appName = try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                packageName
            }
            val appIcon = try {
                packageManager.getApplicationIcon(packageName)
            } catch (_: Exception) {
                null
            }

            val lifecycleOwner = OverlayLifecycleOwner().apply {
                onCreate()
                onResume()
            }
            currentLifecycleOwner = lifecycleOwner

            val composeView = ComposeView(context).apply {
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(lifecycleOwner)
                setViewTreeSavedStateRegistryOwner(lifecycleOwner)
                setContent {
                    val isNight = SessionManager.isNightLockdownActive(
                        preferences.isScheduleEnabled,
                        preferences.scheduleStartHour,
                        preferences.scheduleEndHour
                    )
                    val allowBiometrics = preferences.isBiometricsEnabled && !(isNight && preferences.isNightLockdownPinOnly) && !preferences.isPinOnly(packageName)

                    LockOverlayScreen(
                        targetPackage = packageName,
                        targetAppName = appName,
                        targetIcon = appIcon,
                        isDecoyEnabled = preferences.isDecoyCrashEnabled,
                        isBiometricEnabled = allowBiometrics,
                        isScramblePinEnabled = preferences.isScramblePinEnabled,
                        lockoutSecondsProvider = { SessionManager.getRemainingLockoutSeconds(packageName) },
                        attemptsUntilLockoutProvider = { SessionManager.getAttemptsUntilLockout(packageName) },
                        onUnlockSuccess = {
                            // SEC-001 Fix: Delegate biometric authentication to LockActivity (FragmentActivity)
                            // rather than directly unlocking the application without verifying credentials.
                            val intent = Intent(context, com.pixelguard.applock.ui.lock.LockActivity::class.java).apply {
                                putExtra(com.pixelguard.applock.ui.lock.LockActivity.EXTRA_TARGET_PACKAGE, packageName)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                            }
                            context.startActivity(intent)
                            dismissOverlay()
                        },
                        onPinSubmitted = { pin ->
                            scope.launch {
                                if (SessionManager.isLockedOut(packageName)) {
                                    return@launch
                                }
                                val isCorrect = repository.verifyPin(pin)
                                if (isCorrect) {
                                    SessionManager.resetFailedAttempts(packageName)
                                    SessionManager.markAppUnlocked(packageName)
                                    dismissOverlay()
                                } else {
                                    val attempts = SessionManager.incrementFailedAttempts(packageName)
                                    if (preferences.isIntruderSelfieEnabled && attempts >= preferences.failedAttemptsThreshold) {
                                        captureIntruder(packageName, appName)
                                    }
                                }
                            }
                        },
                        onExitToHome = {
                            goToHomeScreen()
                            dismissOverlay()
                        }
                    )
                }
            }

            val layoutParams = WindowManager.LayoutParams().apply {
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                format = PixelFormat.TRANSLUCENT
                flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                        (if (preferences.isRecentsMaskingEnabled) WindowManager.LayoutParams.FLAG_SECURE else 0)
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                gravity = Gravity.CENTER
            }

            try {
                windowManager.addView(composeView, layoutParams)
                currentOverlayView = composeView
            } catch (e: Exception) {
                Log.e(TAG, "Failed to attach overlay window: ${e.message}")
            }
        }
    }

    private fun captureIntruder(packageName: String, appName: String) {
        val cameraCapture = SilentCameraCapture(context)
        cameraCapture.captureIntruderPhoto { photoFile ->
            scope.launch {
                val record = IntruderRecord(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    targetPackageName = packageName,
                    targetAppName = appName,
                    photoPath = photoFile.absolutePath
                )
                AppLockApplication.instance.repository.addIntruderRecord(record)
            }
        }
    }

    fun dismissOverlay() {
        currentOverlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay view: ${e.message}")
            }
        }
        currentLifecycleOwner?.onDestroy()
        currentLifecycleOwner = null
        currentOverlayView = null
        activePackageName = null
    }

    private fun goToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(homeIntent)
    }

    companion object {
        private const val TAG = "OverlayWindowManager"
    }
}
