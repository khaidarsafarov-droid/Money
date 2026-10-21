package com.example.money.ui.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.example.money.ui.theme.MoneyMotion
import com.example.money.ui.util.systemMotionEnabled

@Composable
fun AnimatedAmount(
    text: String,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
) {
    if (!systemMotionEnabled(LocalContext.current)) {
        Text(
            text = text,
            color = color,
            style = style,
            modifier = modifier,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
        return
    }
    AnimatedContent(
        targetState = text,
        modifier = modifier,
        transitionSpec = {
            (
                fadeIn(tween(MoneyMotion.short, easing = MoneyMotion.emphasizedDecelerate)) +
                    slideInVertically(tween(MoneyMotion.short, easing = MoneyMotion.emphasizedDecelerate)) { it / 5 }
                ) togetherWith (
                fadeOut(tween(MoneyMotion.fadeOut, easing = MoneyMotion.emphasizedAccelerate)) +
                    slideOutVertically(tween(MoneyMotion.fadeOut, easing = MoneyMotion.emphasizedAccelerate)) { -it / 5 }
                )
        },
        label = "amount",
    ) { value ->
        Text(
            text = value,
            color = color,
            style = style,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
        )
    }
}
