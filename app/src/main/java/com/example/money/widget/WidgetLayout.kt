package com.example.money.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 3 home-screen cells. Below this the widget stays a single card (default is 4×2). */
internal val WidgetActiveListMinHeight = 180.dp

internal fun showsWidgetActiveList(height: Dp): Boolean = height >= WidgetActiveListMinHeight

/**
 * Keeps amounts such as "122 115,00 $" on one line inside a half-width cell
 * of a 4-column widget. Shorter amounts can use a larger size when the widget is wide.
 */
internal fun widgetAmountFontSize(width: Dp, textLength: Int): TextUnit = when {
    textLength >= 13 -> 11.sp
    width < 300.dp -> 12.sp
    else -> 14.sp
}
