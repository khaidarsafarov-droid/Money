package com.example.money.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class MoneyExtraColors(
    val negative: Color,
    val negativeContainer: Color,
    val onNegativeContainer: Color,
    val positive: Color,
    val positiveContainer: Color,
)

fun ColorScheme.toMoneyExtraColors(): MoneyExtraColors = MoneyExtraColors(
    negative = error,
    negativeContainer = errorContainer,
    onNegativeContainer = onErrorContainer,
    positive = primary,
    positiveContainer = primaryContainer,
)

val LocalMoneyExtraColors = staticCompositionLocalOf {
    MoneyExtraColors(
        negative = Color(0xFFB3261E),
        negativeContainer = Color(0xFFFBE7E4),
        onNegativeContainer = Color(0xFF5C120E),
        positive = Color(0xFF0B6E5B),
        positiveContainer = Color(0xFFDDF3EC),
    )
}
