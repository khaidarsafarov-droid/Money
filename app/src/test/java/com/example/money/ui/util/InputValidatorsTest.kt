package com.example.money.ui.util

import com.example.money.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InputValidatorsTest {

    @Test
    fun parseAmount_acceptsCommaAndDot() {
        assertEquals(10_050L, InputValidators.parseAmount("100,5"))
        assertEquals(10_050L, InputValidators.parseAmount("100.5"))
    }

    @Test
    fun parseAmount_roundsHalfUpToCents() {
        assertEquals(101L, InputValidators.parseAmount("1.005"))
        assertEquals(100L, InputValidators.parseAmount("1.004"))
    }

    @Test
    fun parseAmount_rejectsZeroAndNegative() {
        assertNull(InputValidators.parseAmount("0"))
        assertNull(InputValidators.parseAmount("-10"))
    }

    @Test
    fun parseAmount_rejectsExponentInfinityNanAndHugeValues() {
        assertNull(InputValidators.parseAmount("Infinity"))
        assertNull(InputValidators.parseAmount("NaN"))
        assertNull(InputValidators.parseAmount("1e9"))
        assertNull(InputValidators.parseAmount("1E2"))
        assertNull(InputValidators.parseAmount("10000000.01"))
        assertEquals(InputValidators.MAX_AMOUNT_CENTS, InputValidators.parseAmount("10000000"))
    }

    @Test
    fun validateDueDate_rejectsDueBeforeGiven() {
        val given = 1_000L
        val due = 500L
        assertEquals(
            R.string.error_due_before_given,
            InputValidators.validateDueDate(given, due),
        )
    }
}
