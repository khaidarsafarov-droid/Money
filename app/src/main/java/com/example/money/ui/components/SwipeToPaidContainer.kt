package com.example.money.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.ui.theme.LocalMoneyExtraColors
import com.example.money.ui.util.MoneyHaptic
import com.example.money.ui.util.moneyHaptic
import kotlin.math.abs

const val SWIPE_PAID_THRESHOLD_FRACTION = 0.6f

private class SwipeOffsetProbe {
    var read: () -> Float = { 0f }
}

fun swipeReachedPaidThreshold(offsetPx: Float, widthPx: Float): Boolean {
    if (widthPx <= 0f) return false
    return abs(offsetPx) >= widthPx * SWIPE_PAID_THRESHOLD_FRACTION
}

fun swipePaidBackgroundAlpha(armed: Boolean, progress: Float): Float {
    val fraction = progress.coerceIn(0f, 1f)
    return if (armed) 1f else 0.25f * fraction
}

@Composable
fun SwipePaidBackdrop(
    armed: Boolean,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val positive = LocalMoneyExtraColors.current.positive
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(positive.copy(alpha = swipePaidBackgroundAlpha(armed, progress))),
        contentAlignment = Alignment.CenterEnd,
    ) {
        val stretch = if (armed) 1.15f else 0.9f
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = stringResource(R.string.a11y_mark_paid),
            tint = Color.White,
            modifier = Modifier
                .padding(end = 24.dp)
                .size(24.dp)
                .graphicsLayer {
                    scaleX = stretch
                    scaleY = stretch
                },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun SwipeToPaidContainer(
    enabled: Boolean,
    onMarkPaid: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    var widthPx by remember { mutableFloatStateOf(0f) }
    val offsetProbe = remember { SwipeOffsetProbe() }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart &&
                swipeReachedPaidThreshold(offsetProbe.read(), widthPx)
            ) {
                view.moneyHaptic(MoneyHaptic.Confirm)
                onMarkPaid()
            }
            false
        },
        positionalThreshold = { distance -> distance * SWIPE_PAID_THRESHOLD_FRACTION },
    )
    offsetProbe.read = { runCatching { dismissState.requireOffset() }.getOrDefault(0f) }
    SwipeToDismissBox(
        modifier = modifier.onSizeChanged { widthPx = it.width.toFloat() },
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = enabled,
        gesturesEnabled = enabled,
        backgroundContent = {
            val armed = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            val progress = abs(dismissState.progress).coerceIn(0f, 1f)
            SwipePaidBackdrop(armed = armed, progress = progress)
        },
        content = { content() },
    )
}
