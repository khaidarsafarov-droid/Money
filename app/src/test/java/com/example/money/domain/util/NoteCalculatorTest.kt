package com.example.money.domain.util

import com.example.money.domain.model.FinanceNoteLine
import com.example.money.settings.CurrencyRegion
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteCalculatorTest {

    @Test
    fun totalIsTheSumOfLines() {
        val lines = listOf(
            FinanceNoteLine(id = 1, noteId = 7, label = "Мука", amountCents = 20_000),
            FinanceNoteLine(id = 2, noteId = 7, label = "Масло", amountCents = 30_000),
            FinanceNoteLine(id = 3, noteId = 8, label = "Другое", amountCents = 99_000),
        )
        val house = lines.filter { it.noteId == 7L }
        assertEquals(50_000L, NoteCalculator.totalCents(house))
    }

    @Test
    fun currencyAcceptsSomoniAndAnyIsoCode() {
        assertEquals("TJS", CurrencyRegion.fromCode("TJS").currencyCode)
        assertEquals("USD", CurrencyRegion.fromCode("US").currencyCode)
        assertEquals("EUR", CurrencyRegion.fromCode("eur").currencyCode)
        assertEquals(CurrencyRegion.US, CurrencyRegion.fromCode("not-a-currency"))
        assertEquals(true, CurrencyRegion.catalog().any { it.currencyCode == "TJS" })
    }
}
