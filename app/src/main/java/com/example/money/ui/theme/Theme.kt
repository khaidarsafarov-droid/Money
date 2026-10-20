package com.example.money.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
private val MoneyShapes = Shapes(
    extraSmall = RoundedCornerShape(MoneyRadius.xs),
    small = RoundedCornerShape(MoneyRadius.sm),
    medium = RoundedCornerShape(MoneyRadius.md),
    large = RoundedCornerShape(MoneyRadius.lg),
    extraLarge = RoundedCornerShape(MoneyRadius.xl),
)

@Composable
fun MoneyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = MoneyShapes,
        content = content,
    )
}
