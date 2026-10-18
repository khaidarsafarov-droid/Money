package com.example.money.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Canonical type scale: 32 / 24 / 18 / 16 / 12.
 * Digits use tabular figures so amounts stay aligned.
 */
object MoneyTypeScale {
    private const val tabular = "tnum"

    val display = TextStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        fontFeatureSettings = tabular,
    )
    val heading = TextStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        fontFeatureSettings = tabular,
    )
    val subheading = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        fontFeatureSettings = tabular,
    )
    val body = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        fontFeatureSettings = tabular,
    )
    val caption = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
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
