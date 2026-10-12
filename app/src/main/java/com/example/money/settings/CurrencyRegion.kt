package com.example.money.settings

import java.util.Currency
import java.util.Locale

/**
 * Any ISO currency chosen in settings. Amounts across the app use this code.
 */
class CurrencyRegion private constructor(
    val currencyCode: String,
) {
    val name: String get() = currencyCode

    val locale: Locale get() = Locale.getDefault()

    val symbol: String
        get() = currency.symbol

    val displayName: String
        get() = currency.getDisplayName(Locale.getDefault())

    private val currency: Currency
        get() = Currency.getInstance(currencyCode)

    override fun equals(other: Any?): Boolean =
        other is CurrencyRegion && other.currencyCode == currencyCode

    override fun hashCode(): Int = currencyCode.hashCode()

    override fun toString(): String = currencyCode

    companion object {
        val US: CurrencyRegion = CurrencyRegion("USD")

        private val preferredCodes = listOf(
            "TJS",
            "USD",
            "EUR",
            "RUB",
            "UZS",
            "KZT",
            "KGS",
            "UAH",
            "TRY",
            "GBP",
            "CNY",
            "AED",
        )

        fun fromCode(code: String): CurrencyRegion {
            val normalized = when (val raw = code.trim().uppercase(Locale.ROOT)) {
                "US" -> "USD"
                else -> raw
            }
            return if (isIsoCurrency(normalized)) CurrencyRegion(normalized) else US
        }

        fun catalog(): List<CurrencyRegion> {
            val available = Currency.getAvailableCurrencies()
                .map { it.currencyCode }
                .filter { isIsoCurrency(it) }
                .sorted()
            return (preferredCodes + available)
                .distinct()
                .map { CurrencyRegion(it) }
        }

        private fun isIsoCurrency(code: String): Boolean {
            if (code.length != 3 || code.any { !it.isLetter() }) return false
            return try {
                Currency.getInstance(code)
                true
            } catch (_: IllegalArgumentException) {
                false
            }
        }
    }
}
