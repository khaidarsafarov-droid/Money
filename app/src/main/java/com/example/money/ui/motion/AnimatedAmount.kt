package com.example.money.ui.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
        Text(text = text, color = color, style = style, modifier = modifier, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
        return
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        text.forEachIndexed { index, character ->
            AnimatedContent(
                targetState = character,
                transitionSpec = {
                    slideInVertically(MoneyMotion.springSpec(true)) { it } togetherWith
                        slideOutVertically(MoneyMotion.springSpec(true)) { -it }
                },
                label = "amount-$index",
            ) { glyph ->
                Text(
                    text = glyph.toString(),
                    color = color,
                    style = style,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}
