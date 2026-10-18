package com.example.money.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DebtBalanceTest {

    @Test
    fun remainingIsSumOfAdvancesMinusPayments() {
        assertEquals(
            1_300_00L,
            DebtBalance.remainingCents(
                advanceCents = listOf(500_00L, 500_00L, 500_00L),
                paymentCents = listOf(200_00L),
            ),
        )
    }

    @Test
    fun extraAdvanceIncreasesTheSameBalance() {
        val afterThree = DebtBalance.remainingCents(listOf(500_00L, 500_00L, 500_00L), emptyList())
        val afterExtra = DebtBalance.remainingCents(
            listOf(500_00L, 500_00L, 500_00L, 200_00L),
            emptyList(),
        )
        assertEquals(1_500_00L, afterThree)
        assertEquals(1_700_00L, afterExtra)
    }

    @Test
    fun remainingDoesNotGoBelowZero() {
        assertEquals(0L, DebtBalance.remainingCents(listOf(100_00L), listOf(250_00L)))
    }
}
