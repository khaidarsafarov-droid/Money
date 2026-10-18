package com.example.money.domain.util

import com.example.money.domain.model.DashboardGoalMetrics
import com.example.money.domain.model.DebtStatus
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanProgressTest {

    @Test
    fun combinesIOwePaidWithGoalSavings() {
        val debts = listOf(
            debt(type = DebtType.DEBTOR, target = 10_000, current = 5_000),
            debt(type = DebtType.CREDITOR, target = 8_000, current = 8_000),
        )
        val goals = DashboardGoalMetrics(
            totalAccumulatedCents = 2_000,
            totalRemainingToGoalCents = 2_000,
        )

        assertEquals(50, GoalPsychologyEngines.planProgressPercent(debts, goals))
    }

    @Test
    fun emptyPlanIsZero() {
        assertEquals(
            0,
            GoalPsychologyEngines.planProgressPercent(emptyList(), DashboardGoalMetrics(0, 0)),
        )
    }

    @Test
    fun creditorBalancesDoNotChangeTheScore() {
        val goals = DashboardGoalMetrics(totalAccumulatedCents = 2_500, totalRemainingToGoalCents = 7_500)
        val withoutCreditor = listOf(debt(type = DebtType.DEBTOR, target = 0, current = 0))
        val withCreditor = withoutCreditor + debt(type = DebtType.CREDITOR, target = 9_000, current = 1_000)

        assertEquals(25, GoalPsychologyEngines.planProgressPercent(withoutCreditor, goals))
        assertEquals(25, GoalPsychologyEngines.planProgressPercent(withCreditor, goals))
    }

    @Test
    fun paidIOweDebtsStayInThePool() {
        val paid = debt(type = DebtType.DEBTOR, target = 10_000, current = 10_000)
            .copy(status = DebtStatus.COMPLETED)
        assertEquals(
            100,
            GoalPsychologyEngines.planProgressPercent(listOf(paid), DashboardGoalMetrics(0, 0)),
        )
    }

    @Test
    fun truncatesTowardZero() {
        val debts = listOf(debt(type = DebtType.DEBTOR, target = 3, current = 2))
        assertEquals(66, GoalPsychologyEngines.planProgressPercent(debts, DashboardGoalMetrics(0, 0)))
    }

    private fun debt(type: DebtType, target: Long, current: Long) = Debtor(
        name = type.name,
        targetAmountCents = target,
        currentAmountCents = current,
        debtType = type,
        dateGiven = 0L,
        dateDue = 1L,
    )
}
