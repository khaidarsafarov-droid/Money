package com.example.money.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object MoneyMotion {
    const val short = 150
    const val medium = 300
    const val long = 500
    const val fadeOut = 90
    const val fadeIn = 210
    const val pressScale = 0.96f

    val emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val emphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val emphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    fun <T> spec(enabled: Boolean, durationMillis: Int = medium): FiniteAnimationSpec<T> =
        if (enabled) tween(durationMillis = durationMillis, easing = emphasized) else snap()

    fun <T> springSpec(enabled: Boolean): FiniteAnimationSpec<T> =
        if (enabled) {
            spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
        } else {
            snap()
        }

    fun <T> pressSpec(enabled: Boolean): FiniteAnimationSpec<T> =
        if (enabled) {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        } else {
            snap()
        }
}

fun <T> motionSpec(enabled: Boolean, durationMillis: Int = MoneyMotion.medium): AnimationSpec<T> =
    MoneyMotion.spec(enabled, durationMillis)
