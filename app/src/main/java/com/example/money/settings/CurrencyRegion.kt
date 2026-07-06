package com.example.money.settings

import java.util.Locale

enum class CurrencyRegion(val locale: Locale, val currencyCode: String) {
    US(Locale.US, "USD"),
    ;

    companion object {
        /** Приложение работает только в USD. */
        fun fromCode(@Suppress("UNUSED_PARAMETER") code: String): CurrencyRegion = US
    }
}
