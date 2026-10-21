package com.example.money.ui.motion

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.money.ui.util.systemMotionEnabled

val LocalMoneySharedTransition = compositionLocalOf<SharedTransitionScope?> { null }

val LocalMoneyVisibility = compositionLocalOf<AnimatedVisibilityScope?> { null }

@Composable
fun Modifier.moneySharedBounds(key: Any): Modifier {
    val shared = LocalMoneySharedTransition.current ?: return this
    val visibility = LocalMoneyVisibility.current ?: return this
    if (!systemMotionEnabled(LocalContext.current)) return this
    val shape = RoundedCornerShape(24.dp)
    return with(shared) {
        this@moneySharedBounds
            .sharedBounds(
                sharedContentState = rememberSharedContentState(key),
                animatedVisibilityScope = visibility,
                boundsTransform = { _, _ ->
                    spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                },
                clipInOverlayDuringTransition = OverlayClip(shape),
            )
            .clip(shape)
    }
}
