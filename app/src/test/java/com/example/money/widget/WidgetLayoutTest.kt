package com.example.money.widget

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetLayoutTest {

    @Test
    fun heroAmountGrowsOnAWideCardAndShrinksForLongText() {
        assertEquals(30.sp, widgetHeroAmountSize(320.dp, 150.dp, "237,00 $".length))
        assertEquals(30.sp, widgetHeroAmountSize(320.dp, 150.dp, "33 779,20 $".length))
        assertEquals(26.sp, widgetHeroAmountSize(320.dp, 150.dp, 12))
        assertEquals(16.sp, widgetHeroAmountSize(160.dp, 110.dp, "0,00 $".length))
        assertEquals(20.sp, widgetHeroAmountSize(320.dp, 150.dp, 20))
        assertEquals(13.sp, widgetHeroAmountSize(160.dp, 110.dp, 20))
        val secondary = widgetSecondaryAmountSize(26.sp)
        assertEquals(20.sp, secondary)
        assertTrue(secondary.value < 26f)
    }

    @Test
    fun fourByTwoUsesTwoAmountColumnsAndAFullWidthGoalBar() {
        assertEquals(false, widgetSideBySideAmounts(220.dp))
        assertEquals(true, widgetSideBySideAmounts(240.dp))
        assertEquals(true, widgetSideBySideAmounts(320.dp))
        assertEquals(100.dp, widgetGoalTrackWidth(124.dp, 12.dp))
        assertEquals(292.dp, widgetGoalTrackWidth(320.dp, 14.dp))
        assertEquals(33.dp, widgetProgressFillWidth(100.dp, 33))
        assertEquals(0.dp, widgetProgressFillWidth(100.dp, 0))
        assertEquals("33%", featuredGoalPercentLabel(33))
        assertEquals(14.dp, widgetContentPadding(150.dp))
        assertEquals(10.dp, widgetContentPadding(110.dp))
        assertEquals(34.dp, widgetPlusSize(150.dp))
    }
}
