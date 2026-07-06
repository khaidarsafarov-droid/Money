package com.example.money.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MoneyColorScheme = lightColorScheme(
    primary = GoldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3E6C8),
    onPrimaryContainer = BrownText,
    secondary = BrownMuted,
    onSecondary = Color.White,
    background = CreamBackground,
    onBackground = BrownText,
    surface = CreamSurface,
    onSurface = BrownText,
    surfaceVariant = Color(0xFFEFE6DA),
    onSurfaceVariant = BrownMuted,
    error = OverdueRed,
    onError = Color.White,
    outline = CardBorder,
)

private val MoneyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun MoneyTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = MoneyColorScheme,
        typography = Typography,
        shapes = MoneyShapes,
        content = content,
    )
}
