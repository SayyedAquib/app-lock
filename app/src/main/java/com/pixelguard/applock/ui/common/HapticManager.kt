package com.pixelguard.applock.ui.common

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticManager(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private fun isSystemHapticAllowed(): Boolean {
        return try {
            val hapticEnabled = android.provider.Settings.System.getInt(
                context.contentResolver,
                android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED,
                1
            ) != 0
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            val notSilent = audioManager?.ringerMode != android.media.AudioManager.RINGER_MODE_SILENT
            hapticEnabled && notSilent
        } catch (_: Exception) {
            true
        }
    }

    fun tapClick(enabled: Boolean = true) {
        if (!enabled || !isSystemHapticAllowed() || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(15)
        }
    }

    fun keypadTick(enabled: Boolean = true) {
        if (!enabled || !isSystemHapticAllowed() || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(8)
        }
    }

    fun unlockSuccess(enabled: Boolean = true) {
        if (!enabled || !isSystemHapticAllowed() || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 25, 40, 25), -1)
        }
    }

    fun authFailed(enabled: Boolean = true) {
        if (!enabled || !isSystemHapticAllowed() || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(80)
        }
    }
}
