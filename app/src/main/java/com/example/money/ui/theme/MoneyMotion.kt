package com.example.money.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object MoneyMotion {
    /** M3 short3. */
    const val short = 150

    /** M3 medium1. */
    const val medium = 250

    /** M3 medium4. */
    const val long = 400
    const val fadeOut = 100
    const val fadeIn = 150
    const val pressScale = 0.97f

    /** Critically damped: settles without bounce. */
    const val springDamping = 1f
    const val springStiffness = Spring.StiffnessMedium
    const val pressDamping = Spring.DampingRatioNoBouncy
    const val pressStiffness = Spring.StiffnessMedium

    /** Sheet travel, about a quarter second, no overshoot. */
    const val sheetStiffness = 700f

    val emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val emphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val emphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    fun <T> spec(enabled: Boolean, durationMillis: Int = medium): FiniteAnimationSpec<T> =
        if (enabled) tween(durationMillis = durationMillis, easing = emphasized) else snap()

    fun <T> springSpec(enabled: Boolean): FiniteAnimationSpec<T> =
        if (enabled) {
            spring(dampingRatio = springDamping, stiffness = springStiffness)
        } else {
            snap()
        }

    fun <T> pressSpec(enabled: Boolean): FiniteAnimationSpec<T> =
        if (enabled) {
            spring(dampingRatio = pressDamping, stiffness = pressStiffness)
        } else {
            snap()
        }

    fun <T> sheetSpec(enabled: Boolean): FiniteAnimationSpec<T> =
        if (enabled) {
            spring(dampingRatio = springDamping, stiffness = sheetStiffness)
        } else {
            snap()
        }
}

fun <T> motionSpec(enabled: Boolean, durationMillis: Int = MoneyMotion.medium): AnimationSpec<T> =
    MoneyMotion.spec(enabled, durationMillis)
