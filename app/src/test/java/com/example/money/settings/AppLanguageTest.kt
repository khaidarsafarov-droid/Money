package com.example.money.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun fromStored_parsesKnownValues() {
        assertEquals(AppLanguage.RU, AppLanguage.fromStored("RU"))
        assertEquals(AppLanguage.EN, AppLanguage.fromStored("en"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromStored(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromStored("SYSTEM"))
    }
}
