package com.example.money.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Canonical type scale: 24 / 18 / 14 for heading / subheading / body.
 */
object MoneyTypeScale {
    val display = TextStyle(
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Bold,
    )
    val heading = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val subheading = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    )
    val body = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    )
    val caption = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
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
