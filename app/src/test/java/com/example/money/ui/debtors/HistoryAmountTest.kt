package com.example.money.ui.debtors

import com.example.money.ui.components.signedNetBalance
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryAmountTest {

    @Test
    fun historyAmountsKeepDirection() {
        assertEquals("− 12 000 ₽", signedHistoryAmount("12 000 ₽", took = true, hidden = false))
        assertEquals("+ 8 000 ₽", signedHistoryAmount("8 000 ₽", took = false, hidden = false))
        assertEquals("••••", signedHistoryAmount("••••", took = true, hidden = true))
    }

    @Test
    fun netBalancePrefixesOnlyARealDifference() {
        assertEquals("+18 400 ₽", signedNetBalance("18 400 ₽", netCents = 1_840_000, hidden = false))
        assertEquals("-6 000 ₽", signedNetBalance("6 000 ₽", netCents = -600_000, hidden = false))
        assertEquals("0 ₽", signedNetBalance("0 ₽", netCents = 0, hidden = false))
        assertEquals("••••", signedNetBalance("••••", netCents = 100, hidden = true))
    }
}
