package com.example.money.widget

import android.content.Context
import android.os.Build
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Amounts stay on one line. Nothing drops below 12sp.
 */
internal fun widgetAmountFontSize(width: Dp, textLength: Int): TextUnit = when {
    textLength >= 16 -> 12.sp
    textLength >= 12 -> 14.sp
    width < 220.dp -> 16.sp
    else -> 18.sp
}

internal fun widgetOuterRadius(context: Context): Dp {
    if (Build.VERSION.SDK_INT < 31) return 16.dp
    val px = context.resources.getDimension(android.R.dimen.system_app_widget_background_radius)
    return (px / context.resources.displayMetrics.density).dp
}
