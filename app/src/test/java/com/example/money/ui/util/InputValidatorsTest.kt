package com.example.money.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InputValidatorsTest {

    @Test
    fun parseAmount_acceptsCommaAndDot() {
        assertEquals(100.5, InputValidators.parseAmount("100,5")!!, 0.001)
        assertEquals(100.5, InputValidators.parseAmount("100.5")!!, 0.001)
    }

    @Test
    fun parseAmount_rejectsZeroAndNegative() {
        assertNull(InputValidators.parseAmount("0"))
        assertNull(InputValidators.parseAmount("-10"))
    }

    @Test
    fun validateDueDate_rejectsDueBeforeGiven() {
        val given = 1_000L
        val due = 500L
        assertEquals(
            "Срок не может быть раньше даты выдачи",
            InputValidators.validateDueDate(given, due),
        )
    }
}
