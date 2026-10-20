package com.example.money.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Canonical type scale: 36 / 22 / 20 / 16 / 14 / 13 / 12.
 * Digits use tabular figures so amounts stay aligned.
 */
object MoneyTypeScale {
    private const val tabular = "tnum"

    val display = TextStyle(
        fontFamily = OnestFamily,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.8).sp,
        fontFeatureSettings = tabular,
    )
    val heading = TextStyle(
        fontFamily = OnestFamily,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        fontFeatureSettings = tabular,
    )
    val subheading = TextStyle(
        fontFamily = OnestFamily,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        fontFeatureSettings = tabular,
    )
    val body = TextStyle(
        fontFamily = OnestFamily,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        fontFeatureSettings = tabular,
    )
    val caption = TextStyle(
        fontFamily = OnestFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        fontFeatureSettings = tabular,
    )
    val amountInput = TextStyle(
        fontFamily = OnestFamily,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.8).sp,
        fontFeatureSettings = tabular,
    )
}

@Composable
fun moneyDisplayStyle(): TextStyle = MoneyTypeScale.display.copy(
    color = MaterialTheme.colorScheme.onSurface,
)

@Composable
fun moneyHeadingStyle(): TextStyle = MoneyTypeScale.heading.copy(
    color = MaterialTheme.colorScheme.onSurface,
)

@Composable
fun moneySubheadingStyle(): TextStyle = MoneyTypeScale.subheading.copy(
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
fun moneyBodyStyle(): TextStyle = MoneyTypeScale.body.copy(
    color = MaterialTheme.colorScheme.onSurface,
)

@Composable
fun moneyCaptionStyle(): TextStyle = MoneyTypeScale.caption.copy(
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)
