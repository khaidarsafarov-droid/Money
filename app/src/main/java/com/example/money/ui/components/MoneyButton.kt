package com.example.money.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.money.ui.motion.pressScale
import com.example.money.ui.theme.MoneySpacing

enum class MoneyButtonPriority {
    Primary,
    Secondary,
    Tertiary,
}

@Composable
fun MoneyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    priority: MoneyButtonPriority = MoneyButtonPriority.Primary,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val minSizeModifier = modifier.pressScale().defaultMinSize(
        minWidth = MoneySpacing.minTouchTarget,
        minHeight = MoneySpacing.minTouchTarget,
    )
    val contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)

    when (priority) {
        MoneyButtonPriority.Primary -> {
            Button(
                onClick = onClick,
                modifier = minSizeModifier,
                enabled = enabled,
                shape = RoundedCornerShape(50),
                contentPadding = contentPadding,
            ) {
                MoneyButtonContent(text = text, icon = icon)
            }
        }
        MoneyButtonPriority.Secondary -> {
            OutlinedButton(
                onClick = onClick,
                modifier = minSizeModifier,
                enabled = enabled,
                shape = RoundedCornerShape(50),
                contentPadding = contentPadding,
            ) {
                MoneyButtonContent(text = text, icon = icon)
            }
        }
        MoneyButtonPriority.Tertiary -> {
            TextButton(
                onClick = onClick,
                modifier = minSizeModifier,
                enabled = enabled,
                contentPadding = contentPadding,
            ) {
                MoneyButtonContent(text = text, icon = icon)
            }
        }
    }
}

@Composable
private fun MoneyButtonContent(
    text: String,
    icon: ImageVector?,
) {
    if (icon != null) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 8.dp),
            maxLines = 1,
        )
    } else {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}
