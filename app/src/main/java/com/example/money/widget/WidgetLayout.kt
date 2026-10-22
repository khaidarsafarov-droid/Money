package com.example.money.widget

import android.content.Context
import android.os.Build
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.money.ui.util.Formatters

/**
 * Hero amount on the green card. Long figures step down so they stay on one line.
 * Nothing drops below 13sp.
 */
internal fun widgetHeroAmountSize(width: Dp, height: Dp, textLength: Int): TextUnit {
    val base = when {
        height < 130.dp || width < 200.dp -> 16
        height >= 200.dp && width >= 300.dp -> 34
        width >= 300.dp -> 30
        height >= 180.dp -> 24
        else -> 22
    }
    val scaled = when {
        textLength >= 18 -> base - 10
        textLength >= 14 -> base - 6
        textLength >= 12 -> base - 4
        else -> base
    }.coerceIn(13, 36)
    return scaled.sp
}

internal fun widgetSecondaryAmountSize(hero: TextUnit): TextUnit =
    (hero.value * 0.78f).toInt().coerceIn(13, 28).sp

/** Two amount columns need a 4×2-wide cell. Narrower cards stack. */
internal fun widgetSideBySideAmounts(width: Dp): Boolean = width >= 240.dp

internal fun widgetContentPadding(height: Dp): Dp = when {
    height < 130.dp -> 10.dp
    height < 180.dp -> 14.dp
    else -> 18.dp
}

internal fun widgetPlusSize(height: Dp): Dp = when {
    height < 130.dp -> 26.dp
    height < 180.dp -> 34.dp
    else -> 40.dp
}

/** The goal bar spans the card. Extra height stays inside the card, above the bar. */
internal fun widgetGoalTrackWidth(widgetWidth: Dp, horizontalPadding: Dp): Dp =
    (widgetWidth - horizontalPadding * 2).coerceAtLeast(0.dp)

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
