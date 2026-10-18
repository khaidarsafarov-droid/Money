package com.example.money.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingNameTest {

    @Test
    fun combinePersonName_joinsFirstAndLast() {
        assertEquals("Иван Петров", combinePersonName(" Иван ", "Петров"))
        assertEquals("Анна", combinePersonName("Анна", " "))
    }

    @Test
    fun continueRequiresBothNameParts() {
        assertTrue(isPersonNamePart("Иван", required = true))
        assertTrue(isPersonNamePart("Петров-Сидоров", required = true))
        assertFalse(isPersonNamePart("", required = true))
        assertFalse(isPersonNamePart("А", required = true))
        assertFalse(isPersonNamePart("Ivan1", required = true))
    }
}
