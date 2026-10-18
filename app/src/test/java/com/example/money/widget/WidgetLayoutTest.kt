package com.example.money.widget

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetLayoutTest {

    @Test
    fun longAmountsUseASmallerSingleLineSize() {
        val longAmount = "122 115,00 $"
        assertEquals(12, longAmount.length)
        assertEquals(12.sp, widgetAmountFontSize(250.dp, longAmount.length))
        assertEquals(14.sp, widgetAmountFontSize(320.dp, longAmount.length))
        assertEquals(11.sp, widgetAmountFontSize(360.dp, "1 222 115,00 $".length))
        assertEquals(12.sp, widgetAmountFontSize(250.dp, "22 115,00 $".length))
        assertEquals(14.sp, widgetAmountFontSize(320.dp, "0,00 $".length))
    }
}
