package com.example.money.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings

enum class HapticStrength {
    LIGHT,
    MEDIUM,
}

fun vibrateShort(context: Context) {
    vibrate(context, HapticStrength.MEDIUM)
}

fun vibrateLight(context: Context) {
    vibrate(context, HapticStrength.LIGHT)
}

fun withLightHaptic(
    context: Context,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
): (Boolean) -> Unit = { checked ->
    if (enabled) vibrateLight(context)
    onCheckedChange(checked)
}

fun vibrate(context: Context, strength: HapticStrength) {
    if (!systemHapticsEnabled(context)) return
    val vibrator = systemVibrator(context) ?: return
    if (!vibrator.hasVibrator()) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val effect = if (strength == HapticStrength.LIGHT) {
            VibrationEffect.EFFECT_TICK
        } else {
            VibrationEffect.EFFECT_CLICK
        }
        vibrator.vibrate(VibrationEffect.createPredefined(effect))
        return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val duration = if (strength == HapticStrength.LIGHT) 12L else 28L
        val amplitude = if (vibrator.hasAmplitudeControl()) {
            if (strength == HapticStrength.LIGHT) 48 else 140
        } else {
            VibrationEffect.DEFAULT_AMPLITUDE
        }
        vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(if (strength == HapticStrength.LIGHT) 12L else 28L)
    }
}

@Suppress("DEPRECATION")
internal fun systemHapticsEnabled(context: Context): Boolean = try {
    Settings.System.getInt(
        context.contentResolver,
        Settings.System.HAPTIC_FEEDBACK_ENABLED,
        1,
    ) == 1
} catch (_: RuntimeException) {
    true
}

internal fun systemMotionEnabled(context: Context): Boolean = try {
    val resolver = context.contentResolver
    val animator = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    val transition = Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
    animator > 0f && transition > 0f
} catch (_: RuntimeException) {
    true
}

private fun systemVibrator(context: Context): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
}
