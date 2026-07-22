package com.example.money.ui.util

import android.text.format.DateFormat as AndroidDateFormat
import com.example.money.settings.CurrencyRegion
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formatters {

    const val MASKED_AMOUNT = "•••"

    private val locale: Locale
        get() = Locale.getDefault()

    fun currency(amount: Double, region: CurrencyRegion = CurrencyRegion.US, hide: Boolean = false): String =
        if (hide) MASKED_AMOUNT else currencyFormat(region).format(amount)

    fun currencyCompact(amount: Double, region: CurrencyRegion = CurrencyRegion.US, hide: Boolean = false): String {
        if (hide) return MASKED_AMOUNT
        val formatted = currency(amount, region)
        return formatted.replace(".00", "")
    }

    fun currencyFromCents(
        cents: Long,
        @Suppress("UNUSED_PARAMETER") currencyCode: String = "USD",
        hide: Boolean = false,
    ): String = currencyCompact(cents / 100.0, CurrencyRegion.US, hide)

    fun currencyFromCents(cents: Long, region: CurrencyRegion, hide: Boolean = false): String =
        currencyCompact(cents / 100.0, region, hide)

    fun date(millis: Long): String =
        DateFormat.getDateInstance(DateFormat.SHORT, locale).format(Date(millis))

    fun shortDate(millis: Long): String =
        DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(millis))

    fun shortDayMonth(millis: Long): String {
        val calendar = Calendar.getInstance(locale).apply { timeInMillis = millis }
        return AndroidDateFormat.format("d MMM", calendar).toString()
    }

    /** Compact due date: `d MMM`, with year only when not the current year. */
    fun dueDateCompact(millis: Long): String {
        val due = Calendar.getInstance(locale).apply { timeInMillis = millis }
        val now = Calendar.getInstance(locale)
        val pattern = if (due.get(Calendar.YEAR) == now.get(Calendar.YEAR)) "d MMM" else "d MMM yyyy"
        return AndroidDateFormat.format(pattern, due).toString()
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
