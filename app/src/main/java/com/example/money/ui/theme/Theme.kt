package com.example.money.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MoneyLightColorScheme = lightColorScheme(
    primary = MintPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9F6E6),
    onPrimaryContainer = InkText,
    secondary = InkMuted,
    onSecondary = Color.White,
    background = MintBackground,
    onBackground = InkText,
    surface = MintSurface,
    onSurface = InkText,
    surfaceVariant = Color(0xFFD3EEE3),
    onSurfaceVariant = InkMuted,
    error = OverdueRed,
    onError = Color.White,
    outline = CardBorder,
)

private val MoneyDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = MintPrimary,
    onPrimaryContainer = HeroCaption,
    secondary = Color(0xFF9EC9BB),
    onSecondary = DarkOnPrimary,
    background = DarkPage,
    onBackground = HeroCaption,
    surface = DarkSurface,
    onSurface = HeroCaption,
    surfaceVariant = Color(0xFF16483C),
    onSurfaceVariant = Color(0xFF9EC9BB),
    error = Color(0xFFFFB4A8),
    onError = Color(0xFF5C1A12),
    outline = Color(0xFF3D6B5E),
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
        colorScheme = if (isSystemInDarkTheme()) MoneyDarkColorScheme else MoneyLightColorScheme,
        typography = Typography,
        shapes = MoneyShapes,
        content = content,
    )
}
