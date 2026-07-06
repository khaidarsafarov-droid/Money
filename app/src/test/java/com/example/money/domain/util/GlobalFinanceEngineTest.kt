package com.example.money.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalFinanceEngineTest {

    @Test
    fun computeSummary_globalBalanceIsIncomeMinusExpenses() {
        val summary = GlobalFinanceEngine.computeSummary(
            totalDeposits = 500.0,
            totalGoalAllocated = 200.0,
            totalExpenses = 50.0,
        )

        assertEquals(450.0, summary.globalBalance, 0.001)
        assertEquals(250.0, summary.availableBalance, 0.001)
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
        assertEquals(40.0, metrics.totalAccumulated, 0.001)
        assertEquals(60.0, metrics.totalRemainingToGoal, 0.001)
    }

    @Test
    fun canAfford_rejectsAmountAboveAvailableBalance() {
        val summary = GlobalFinanceEngine.computeSummary(
            totalDeposits = 100.0,
            totalGoalAllocated = 40.0,
            totalExpenses = 10.0,
        )

        assertTrue(GlobalFinanceEngine.canAfford(summary, 50.0))
        assertFalse(GlobalFinanceEngine.canAfford(summary, 51.0))
    }
}
