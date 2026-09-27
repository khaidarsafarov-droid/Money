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
    primaryContainer = Color(0xFFD9F6E6),
    onPrimaryContainer = BrownText,
    secondary = BrownMuted,
    onSecondary = Color.White,
    background = CreamBackground,
    onBackground = BrownText,
    surface = CreamSurface,
    onSurface = BrownText,
    surfaceVariant = Color(0xFFD3EEE3),
    onSurfaceVariant = BrownMuted,
    error = OverdueRed,
    onError = Color.White,
    outline = CardBorder,
)

private val MoneyShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
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
