package com.example.money.ui.business.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val BusinessColorScheme = lightColorScheme(
    primary = BusinessColors.PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = BusinessColors.PrimaryDark,
    secondary = BusinessColors.Gray500,
    onSecondary = Color.White,
    background = BusinessColors.Background,
    onBackground = BusinessColors.OnSurface,
    surface = BusinessColors.Surface,
    onSurface = BusinessColors.OnSurface,
    surfaceVariant = BusinessColors.Gray100,
    onSurfaceVariant = BusinessColors.OnSurfaceMuted,
    error = BusinessColors.LossRed,
    onError = Color.White,
    tertiary = BusinessColors.ProfitGreen,
    onTertiary = Color.White,
    outline = BusinessColors.Gray300,
)

private val BusinessShapes = Shapes(
    extraSmall = RoundedCornerShape(BusinessSpacing.cardRadius),
    small = RoundedCornerShape(BusinessSpacing.cardRadius),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun BusinessTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = BusinessColorScheme,
        typography = BusinessTypography,
        shapes = BusinessShapes,
        content = content,
    )
}
