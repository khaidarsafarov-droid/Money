package com.example.money.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

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
    val scheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        shapes = MoneyShapes,
    ) {
        CompositionLocalProvider(
            LocalMoneyExtraColors provides scheme.toMoneyExtraColors(),
            content = content,
        )
    }
}
