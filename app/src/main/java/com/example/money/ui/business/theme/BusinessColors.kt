package com.example.money.ui.business.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Business branch color tokens.
 * Personal mode keeps Milk-Rich theme; Business uses professional blue palette.
 */
object BusinessColors {
    val PrimaryBlue = Color(0xFF1E88E5)
    val PrimaryDark = Color(0xFF0D47A1)
    val ProfitGreen = Color(0xFF43A047)
    val LossRed = Color(0xFFE53935)
    val WarningYellow = Color(0xFFFDD835)
    val Gray100 = Color(0xFFECEFF1)
    val Gray300 = Color(0xFFCFD8DC)
    val Gray500 = Color(0xFF90A4AE)
    val Gray700 = Color(0xFF546E7A)
    val Background = Color(0xFFF5F7FA)
    val Surface = Color(0xFFFFFFFF)
    val OnSurface = Color(0xFF263238)
    val OnSurfaceMuted = Color(0xFF607D8B)
    val InvoiceBlue = PrimaryBlue
    val Success = ProfitGreen
    val Error = LossRed
}

object BusinessSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val cardPadding = 16.dp
    val cardRadius = 8.dp
    val cardElevation = 1.dp
    val sectionGap = 24.dp
    val minTouchTarget = 44.dp
}

enum class BusinessKpiType {
    Revenue,
    Expense,
    Profit,
    Cash,
    Invoice,
    Warning,
}

fun BusinessKpiType.accentColor(): Color = when (this) {
    BusinessKpiType.Revenue -> BusinessColors.ProfitGreen
    BusinessKpiType.Expense -> BusinessColors.LossRed
    BusinessKpiType.Profit -> BusinessColors.PrimaryBlue
    BusinessKpiType.Cash -> BusinessColors.Gray700
    BusinessKpiType.Invoice -> BusinessColors.InvoiceBlue
    BusinessKpiType.Warning -> BusinessColors.WarningYellow
}
