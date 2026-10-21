package com.example.money.ui.motion

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.money.ui.util.systemMotionEnabled
import androidx.compose.animation.AnimatedVisibilityScope

private val sharedSpring = spring<Rect>(
    dampingRatio = 1f,
    stiffness = Spring.StiffnessMedium,
)
private val sharedClip = RoundedCornerShape(24.dp)

val LocalMoneySharedTransition = compositionLocalOf<SharedTransitionScope?> { null }

val LocalMoneyVisibility = compositionLocalOf<AnimatedVisibilityScope?> { null }

@Composable
fun Modifier.moneySharedBounds(key: Any): Modifier {
    val shared = LocalMoneySharedTransition.current ?: return this
    val visibility = LocalMoneyVisibility.current ?: return this
    if (!systemMotionEnabled(LocalContext.current)) return this
    return with(shared) {
        this@moneySharedBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = visibility,
            boundsTransform = { _, _ -> sharedSpring },
            resizeMode = ResizeMode.scaleToBounds(ContentScale.Crop, Alignment.Center),
            clipInOverlayDuringTransition = OverlayClip(sharedClip),
        )
    }
}
