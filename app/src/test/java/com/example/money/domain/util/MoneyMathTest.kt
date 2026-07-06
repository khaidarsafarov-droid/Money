package com.example.money.domain.util

import com.example.money.domain.model.DebtStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyMathTest {

    @Test
    fun processDebtPayment_partialStatus() {
        val status = MoneyMath.resolveDebtStatus(targetCents = 10_000, currentCents = 5_000)
        assertEquals(DebtStatus.PARTIAL, status)
    }

    @Test
    fun processDebtPayment_completedStatus() {
        val status = MoneyMath.resolveDebtStatus(targetCents = 10_000, currentCents = 10_000)
        assertEquals(DebtStatus.COMPLETED, status)
    }

    @Test
    fun remainingCents_neverNegative() {
        assertEquals(0L, MoneyMath.remainingCents(5_000, 6_000))
        assertEquals(2_500L, MoneyMath.remainingCents(5_000, 2_500))
    }

    @Test
    fun toCents_avoidsFloatDrift() {
        assertEquals(5050L, MoneyMath.toCents(50.50))
    }
}
