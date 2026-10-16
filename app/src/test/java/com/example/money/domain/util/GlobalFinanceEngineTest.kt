package com.example.money.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalFinanceEngineTest {

    @Test
    fun computeSummary_globalBalanceIsIncomeMinusExpenses() {
        val summary = GlobalFinanceEngine.computeSummary(
            totalDepositsCents = 50_000L,
            totalGoalAllocatedCents = 20_000L,
            totalExpensesCents = 5_000L,
        )

        assertEquals(45_000L, summary.globalBalanceCents)
        assertEquals(25_000L, summary.availableBalanceCents)
    }

    @Test
    fun goalNetCents_subtractsGoalExpensesAndStaysAtZero() {
        assertEquals(15_000L, GlobalFinanceEngine.goalNetCents(20_000L, 5_000L))
        assertEquals(0L, GlobalFinanceEngine.goalNetCents(1_000L, 4_000L))
    }

    @Test
    fun activeGoalMetrics_sumsRemainingForActiveGoals() {
        val goals = listOf(
            com.example.money.domain.model.SavingsGoal(
                id = 1,
                name = "A",
                targetAmountCents = 10_000,
                currentAmountCents = 4_000,
                updatedAt = 0L,
            ),
            com.example.money.domain.model.SavingsGoal(
                id = 2,
                name = "B",
                targetAmountCents = 5_000,
                currentAmountCents = 5_000,
                updatedAt = 0L,
            ),
        )

        val metrics = GlobalFinanceEngine.activeGoalMetrics(goals)
        assertEquals(4_000L, metrics.totalAccumulatedCents)
        assertEquals(6_000L, metrics.totalRemainingToGoalCents)
    }

    @Test
    fun canAfford_rejectsAmountAboveAvailableBalance() {
        val summary = GlobalFinanceEngine.computeSummary(
            totalDepositsCents = 10_000L,
            totalGoalAllocatedCents = 4_000L,
            totalExpensesCents = 1_000L,
        )

        assertTrue(GlobalFinanceEngine.canAfford(summary, 5_000L))
        assertFalse(GlobalFinanceEngine.canAfford(summary, 5_001L))
    }
}
