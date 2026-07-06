package com.example.money.ui.util

import android.text.format.DateFormat as AndroidDateFormat
import com.example.money.settings.CurrencyRegion
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formatters {

    private val locale: Locale
        get() = Locale.getDefault()

    fun currency(amount: Double, region: CurrencyRegion = CurrencyRegion.US): String =
        currencyFormat(region).format(amount)

    fun currencyCompact(amount: Double, region: CurrencyRegion = CurrencyRegion.US): String {
        val formatted = currency(amount, region)
        return formatted.replace(".00", "")
    }

    fun currencyFromCents(cents: Long, @Suppress("UNUSED_PARAMETER") currencyCode: String = "USD"): String =
        currencyCompact(cents / 100.0, CurrencyRegion.US)

    fun currencyFromCents(cents: Long, region: CurrencyRegion): String =
        currencyCompact(cents / 100.0, CurrencyRegion.US)

    fun date(millis: Long): String =
        DateFormat.getDateInstance(DateFormat.SHORT, locale).format(Date(millis))

    fun shortDate(millis: Long): String =
        DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(millis))

    fun shortDayMonth(millis: Long): String {
        val calendar = Calendar.getInstance(locale).apply { timeInMillis = millis }
        return AndroidDateFormat.format("d MMM", calendar).toString()
    }

    fun monthYear(millis: Long = System.currentTimeMillis()): String {
        val calendar = Calendar.getInstance(locale).apply { timeInMillis = millis }
        return AndroidDateFormat.format("MMMM yyyy", calendar).toString()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    private fun currencyFormat(region: CurrencyRegion): NumberFormat =
        NumberFormat.getCurrencyInstance(region.locale).apply {
            currency = java.util.Currency.getInstance(region.currencyCode)
        }
}
