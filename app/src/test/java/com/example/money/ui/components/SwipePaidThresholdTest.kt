package com.example.money.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipePaidThresholdTest {

    @Test
    fun paidGestureNeedsSixtyPercentOfTheRow() {
        assertEquals(0.6f, SWIPE_PAID_THRESHOLD_FRACTION, 0.0001f)
        val width = 100f
        val limit = width * SWIPE_PAID_THRESHOLD_FRACTION
        assertFalse(swipeReachedPaidThreshold(offsetPx = -(limit - 1f), widthPx = width))
        assertTrue(swipeReachedPaidThreshold(offsetPx = -limit, widthPx = width))
        assertFalse(swipeReachedPaidThreshold(offsetPx = -80f, widthPx = 0f))
    }

    @Test
    fun backgroundStaysFaintUntilTheGestureArms() {
        assertEquals(0f, swipePaidBackgroundAlpha(armed = false, progress = 0f))
        assertEquals(0.125f, swipePaidBackgroundAlpha(armed = false, progress = 0.5f))
        assertEquals(1f, swipePaidBackgroundAlpha(armed = true, progress = 0.6f))
    }
}
