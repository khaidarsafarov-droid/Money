package com.example.money.ui.motion

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.money.ui.theme.MoneyMotion
import com.example.money.ui.util.systemMotionEnabled
import kotlinx.coroutines.CancellationException

@Composable
fun Modifier.moneySheetMotion(onDismiss: () -> Unit): Modifier {
    val motion = systemMotionEnabled(LocalContext.current)
    val enter = remember { Animatable(if (motion) 0f else 1f) }
    val back = remember { Animatable(0f) }
    LaunchedEffect(motion) {
        if (motion) enter.animateTo(1f, MoneyMotion.sheetSpec(true))
    }
    PredictiveBackHandler(enabled = true) { events ->
        try {
            events.collect { event -> back.snapTo(event.progress) }
            back.snapTo(0f)
            onDismiss()
        } catch (_: CancellationException) {
            back.snapTo(0f)
        }
    }
    return this
        .graphicsLayer {
            val shown = enter.value.coerceIn(0f, 1f)
            val gesture = back.value.coerceIn(0f, 1f)
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
    val back = remember { Animatable(0f) }
    LaunchedEffect(motion) {
        if (motion) enter.animateTo(1f, MoneyMotion.sheetSpec(true))
    }
    PredictiveBackHandler(enabled = true) { events ->
        try {
            events.collect { event -> back.snapTo(event.progress) }
            back.snapTo(0f)
            onDismiss()
        } catch (_: CancellationException) {
            back.snapTo(0f)
        }
    }
    val dismissInteraction = remember { MutableInteractionSource() }
    val sheetInteraction = remember { MutableInteractionSource() }
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val shown = enter.value.coerceIn(0f, 1f)
                    val gesture = back.value.coerceIn(0f, 1f)
                    alpha = (0.32f * shown * (1f - gesture)).coerceIn(0f, 1f)
                }
                .background(Color.Black)
                .clickable(
                    interactionSource = dismissInteraction,
                    indication = null,
                    onClick = onDismiss,
                ),
        )
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.96f)
                .graphicsLayer {
                    val shown = enter.value.coerceIn(0f, 1f)
                    val gesture = back.value.coerceIn(0f, 1f)
                    translationY = (1f - shown) * 180f + gesture * 220f
                }
                .clickable(
                    interactionSource = sheetInteraction,
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
