package com.example.money.ui.business.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Business Design System tokens — single source of truth for the Business module.
 *
 * Principles: minimalism, white surfaces, blue/violet accent, large cards, soft shadows.
 */
object BusinessDesignTokens {

    // Colors — blue / violet Money accent
    val AccentBlue = Color(0xFF1E88E5)
    val AccentViolet = Color(0xFF5E35B1)
    val AccentGradientStart = AccentBlue
    val AccentGradientEnd = AccentViolet

    val SurfaceWhite = Color(0xFFFFFFFF)
    val BackgroundSoft = Color(0xFFF5F7FA)
    val TextPrimary = Color(0xFF263238)
    val TextMuted = Color(0xFF607D8B)

    val SuccessGreen = Color(0xFF43A047)
    val WarningAmber = Color(0xFFFDD835)
    val ErrorRed = Color(0xFFE53935)

    // Shape
    val RadiusCard: Dp = 12.dp
    val RadiusButton: Dp = 10.dp
    val RadiusChip: Dp = 50.dp
    val RadiusHero: Dp = 16.dp

    // Spacing
    val SpaceXs: Dp = 4.dp
    val SpaceSm: Dp = 8.dp
    val SpaceMd: Dp = 16.dp
    val SpaceLg: Dp = 24.dp
    val SpaceXl: Dp = 32.dp
    val SpaceXxl: Dp = 48.dp

    // Elevation
    val ElevationCard: Dp = 2.dp
    val ElevationRaised: Dp = 4.dp

    // Shadows (elevation mapping)
    val ShadowSoft: Dp = ElevationCard
    val ShadowMedium: Dp = ElevationRaised

    // Touch
    val MinTouchTarget: Dp = 48.dp

    // Form
    val InputMinLines: Int = 3
    val ComposerMinLines: Int = 5
}
