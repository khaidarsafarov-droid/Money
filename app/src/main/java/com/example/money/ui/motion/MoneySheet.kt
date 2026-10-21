package com.example.money.ui.motion

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.money.ui.util.systemMotionEnabled
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun Modifier.moneySheetMotion(onDismiss: () -> Unit): Modifier {
    val motion = systemMotionEnabled(LocalContext.current)
    val enter = remember { Animatable(if (motion) 0f else 1f) }
    var back by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(motion) {
        if (motion) enter.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 380f))
    }
    PredictiveBackHandler(enabled = true) { events ->
        try {
            events.collect { event -> back = event.progress }
            back = 0f
            onDismiss()
        } catch (_: CancellationException) {
            back = 0f
        }
    }
    return this
        .graphicsLayer {
            val shown = enter.value.coerceIn(0f, 1f)
            val gesture = back.coerceIn(0f, 1f)
            translationY = (1f - shown) * 72f + gesture * 96f
            val scale = 1f - (1f - shown) * 0.04f - gesture * 0.04f
            scaleX = scale
            scaleY = scale
        }
        .imePadding()
}

@Composable
fun MoneyInWindowSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val motion = systemMotionEnabled(LocalContext.current)
    val enter = remember { Animatable(if (motion) 0f else 1f) }
    var back by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(motion) {
        if (motion) {
            launch { enter.animateTo(1f, spring(dampingRatio = 0.84f, stiffness = 340f)) }
        }
    }
    PredictiveBackHandler(enabled = true) { events ->
        try {
            events.collect { event -> back = event.progress }
            back = 0f
            onDismiss()
        } catch (_: CancellationException) {
            back = 0f
        }
    }
    val shown = enter.value.coerceIn(0f, 1f)
    val gesture = back.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.32f * shown * (1f - gesture)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.96f)
                .graphicsLayer {
                    translationY = (1f - shown) * 180f + gesture * 220f
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .imePadding(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            content()
        }
    }
}
