package com.example.money.ui.util

import com.example.money.settings.CurrencyRegion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FormattersTest {

    @Test
    fun currencyFromCents_usesThePassedCurrencyCode() {
        Formatters.activeRegion = CurrencyRegion.US
        val usd = Formatters.currencyFromCents(1_234L, "USD")
        val tjs = Formatters.currencyFromCents(1_234L, "TJS")
        assertNotEquals(usd, tjs)
        assertEquals(usd, Formatters.currencyFromCents(1_234L, "US"))
    }

    @Test
    fun currencyFromCents_withoutCode_usesTheSettingsRegion() {
        Formatters.activeRegion = CurrencyRegion.fromCode("EUR")
        assertEquals(
            Formatters.currencyFromCents(100L, "EUR"),
            Formatters.currencyFromCents(100L),
        )
        Formatters.activeRegion = CurrencyRegion.US
    }
}
