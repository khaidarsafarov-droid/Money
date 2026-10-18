package com.example.money.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Keeps amounts such as "122 115,00 $" on one line inside a half-width cell
 * of a 4-column widget. Shorter amounts can use a larger size when the widget is wide.
 */
internal fun widgetAmountFontSize(width: Dp, textLength: Int): TextUnit = when {
    textLength >= 13 -> 11.sp
    width < 300.dp -> 12.sp
    else -> 14.sp
}
