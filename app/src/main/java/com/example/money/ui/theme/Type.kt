@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.example.money.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.money.R

private const val TabularFigures = "tnum"

private fun onest(weight: Int) = Font(
    resId = R.font.onest,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val OnestFamily = FontFamily(
    onest(400),
    onest(500),
    onest(600),
)

private fun moneyText(
    size: Int,
    line: Int,
    weight: FontWeight,
    tracking: Float = 0f,
) = TextStyle(
    fontFamily = OnestFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
    fontFeatureSettings = TabularFigures,
)

val Typography = Typography(
    displaySmall = moneyText(36, 44, FontWeight.SemiBold, tracking = -0.8f),
    headlineMedium = moneyText(22, 28, FontWeight.SemiBold),
    titleLarge = moneyText(22, 28, FontWeight.SemiBold),
    titleMedium = moneyText(20, 26, FontWeight.SemiBold),
    titleSmall = moneyText(16, 24, FontWeight.Medium),
    bodyLarge = moneyText(16, 24, FontWeight.Medium),
    bodyMedium = moneyText(14, 20, FontWeight.Normal),
    bodySmall = moneyText(12, 16, FontWeight.Normal),
    labelLarge = moneyText(14, 20, FontWeight.Medium),
    labelMedium = moneyText(13, 18, FontWeight.Medium),
    labelSmall = moneyText(12, 16, FontWeight.Medium),
)
