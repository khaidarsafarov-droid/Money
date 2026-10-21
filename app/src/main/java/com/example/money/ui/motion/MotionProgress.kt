package com.example.money.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.money.ui.theme.MoneyMotion
import com.example.money.ui.util.systemMotionEnabled

/**
 * Animates [target] without recomposing the caller. Read the returned lambda from
 * draw (for example `LinearProgressIndicator(progress = fraction)`), not from composition.
 */
@Composable
fun rememberDrawFraction(target: Float): () -> Float {
    val motion = systemMotionEnabled(LocalContext.current)
    val clamped = target.coerceIn(0f, 1f)
    val anim = remember { Animatable(clamped) }
    LaunchedEffect(clamped, motion) {
        if (motion) {
            anim.animateTo(clamped, MoneyMotion.springSpec(true))
        } else {
            anim.snapTo(clamped)
        }
    }
    return remember { { anim.value } }
}
