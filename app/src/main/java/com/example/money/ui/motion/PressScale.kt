package com.example.money.ui.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import com.example.money.ui.theme.MoneyMotion
import com.example.money.ui.util.MoneyHaptic
import com.example.money.ui.util.moneyHaptic
import com.example.money.ui.util.systemMotionEnabled

@Composable
fun Modifier.pressScale(): Modifier {
    val motion = systemMotionEnabled(LocalContext.current)
    val view = LocalView.current
    val longPressTimeout = LocalViewConfiguration.current.longPressTimeoutMillis
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) MoneyMotion.pressScale else 1f,
        animationSpec = MoneyMotion.pressSpec(motion),
        label = "pressScale",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(longPressTimeout) {
            awaitEachGesture {
                val down = awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial,
                )
                pressed = true
                val start = down.uptimeMillis
                var longFired = false
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val stillDown = event.changes.any { it.id == down.id && it.pressed }
                    if (!stillDown) break
                    val now = event.changes.firstOrNull()?.uptimeMillis ?: start
                    if (!longFired && now - start >= longPressTimeout) {
                        longFired = true
                        view.moneyHaptic(MoneyHaptic.LongPress)
                    }
                }
                pressed = false
            }
        }
}
