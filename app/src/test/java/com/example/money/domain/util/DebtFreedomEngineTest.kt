package com.example.money.domain.util

import com.example.money.domain.model.DebtPayment
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DebtFreedomEngineTest {

    @Test
    fun computeMilestones_nextAt25Percent() {
        val result = DebtFreedomEngine.computeMilestones(2_000, 20_000)
        assertEquals(10, result.progressPercent)
        assertEquals(25, result.nextMilestone?.percent)
        assertEquals(3_000, result.nextMilestone?.centsToReach)
    }

    @Test
    fun calculateFreedomProjection_withActiveDebt() {
        val now = 1_735_689_600_000L
        val debtor = Debtor(
            id = 1,
            name = "Test",
            targetAmountCents = 20_000,
            currentAmountCents = 5_000,
            debtType = DebtType.DEBTOR,
            dateGiven = now,
            dateDue = now + 90L * 86_400_000,
        )
        val projection = DebtFreedomEngine.calculateFreedomProjection(
            debts = listOf(debtor),
            debtType = DebtType.DEBTOR,
            now = now,
        )
        assertTrue(projection.hasActiveDebts)
        assertNotNull(projection.freedomDateMillis)
    }

    @Test
    fun calculateFreedomProjection_respectsDueDateOverPaymentPace() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 7, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val due = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 31, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val debtor = Debtor(
            id = 1,
            name = "Yusuf",
            targetAmountCents = 50_000,
            currentAmountCents = 15_000,
            debtType = DebtType.DEBTOR,
            dateGiven = now,
            dateDue = due,
            paymentHistory = listOf(
                DebtPayment(
                    debtorId = 1,
                    amountCents = 15_000,
                    currency = "USD",
                    paidAt = now,
                ),
            ),
        )

        val projection = DebtFreedomEngine.calculateFreedomProjection(
            debts = listOf(debtor),
            debtType = DebtType.DEBTOR,
            now = now,
        )

        assertEquals(due, projection.freedomDateMillis)
        assertEquals(35_000, projection.totalRemainingCents)
        assertEquals(35_000, projection.monthlyPaymentCents)
        assertEquals(1, projection.monthsRemaining)
    }
}
