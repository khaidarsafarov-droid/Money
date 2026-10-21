package com.example.money.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun Modifier.pressScale(): Modifier {
    val motion = systemMotionEnabled(LocalContext.current)
    val view = LocalView.current
    val longPressTimeout = LocalViewConfiguration.current.longPressTimeoutMillis
    val scale = remember { Animatable(1f) }
    return this
        .graphicsLayer {
            val shown = scale.value
            scaleX = shown
            scaleY = shown
        }
        .pointerInput(motion, longPressTimeout) {
            val input = this
            coroutineScope {
                val scope = this
                input.awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial,
                    )
                    val start = down.uptimeMillis
                    var longFired = false
                    val pressJob = if (motion) {
                        scope.launch {
                            scale.animateTo(MoneyMotion.pressScale, MoneyMotion.pressSpec(true))
                        }
                    } else {
                        null
                    }
                    try {
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
                    } finally {
                        pressJob?.cancel()
                        scope.launch {
                            if (motion) {
                                scale.animateTo(1f, MoneyMotion.pressSpec(true))
                            } else {
                                scale.snapTo(1f)
                            }
                        }
                    }
                }
            }
        }
}
