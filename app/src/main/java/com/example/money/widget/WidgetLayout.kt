package com.example.money.widget

import android.content.Context
import android.os.Build
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.money.ui.util.Formatters

/**
 * Amounts stay on one line. Nothing drops below 12sp.
 */
internal fun widgetAmountFontSize(width: Dp, textLength: Int): TextUnit = when {
    textLength >= 16 -> 12.sp
    textLength >= 12 -> 14.sp
    width < 220.dp -> 16.sp
    else -> 18.sp
}

internal val WidgetWideBreakpoint = 280.dp

internal fun widgetUsesWideLayout(width: Dp): Boolean = width >= WidgetWideBreakpoint

/** Track width inside the card. Wide widgets split debts and the goal. */
internal fun widgetGoalTrackWidth(widgetWidth: Dp, horizontalPadding: Dp, wide: Boolean): Dp {
    val inner = (widgetWidth - horizontalPadding * 2).coerceAtLeast(0.dp)
    if (!wide) return inner
    return ((inner - 12.dp) / 2).coerceAtLeast(0.dp)
}

internal fun widgetProgressFillWidth(trackWidth: Dp, percent: Int): Dp =
    trackWidth * (percent.coerceIn(0, 100) / 100f)

internal fun featuredGoalPercentLabel(percent: Int): String = "${percent.coerceIn(0, 100)}%"

internal fun featuredGoalRatio(snapshot: WidgetGoalSnapshot): String {
    if (!snapshot.hasFeaturedGoal) return ""
    if (snapshot.featuredGoalIsAction) {
        if (snapshot.hideAmounts) return "${Formatters.MASKED_AMOUNT}/${Formatters.MASKED_AMOUNT}"
        return "${snapshot.featuredGoalSavedCents}/${snapshot.featuredGoalTargetCents}"
    }
    val saved = Formatters.currencyFromCents(
        snapshot.featuredGoalSavedCents,
        snapshot.currencyRegion,
        snapshot.hideAmounts,
    )
    val target = Formatters.currencyFromCents(
        snapshot.featuredGoalTargetCents,
        snapshot.currencyRegion,
        snapshot.hideAmounts,
    )
    return "$saved / $target"
}

internal fun widgetOuterRadius(context: Context): Dp {
    if (Build.VERSION.SDK_INT < 31) return 16.dp
    val px = context.resources.getDimension(android.R.dimen.system_app_widget_background_radius)
    return (px / context.resources.displayMetrics.density).dp
}
