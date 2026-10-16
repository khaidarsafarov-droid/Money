package com.example.money.domain.util

import com.example.money.domain.model.DashboardGoalMetrics
import com.example.money.domain.model.GlobalFinanceSummary
import com.example.money.domain.model.GoalKind

object GlobalFinanceEngine {

    fun computeSummary(
        totalDepositsCents: Long,
        totalGoalAllocatedCents: Long,
        totalExpensesCents: Long,
    ): GlobalFinanceSummary = GlobalFinanceSummary(
        totalDepositsCents = totalDepositsCents,
        totalGoalAllocatedCents = totalGoalAllocatedCents,
        totalExpensesCents = totalExpensesCents,
    )

    fun canAfford(summary: GlobalFinanceSummary, amountCents: Long): Boolean {
        if (amountCents <= 0L) return false
        return amountCents <= summary.availableBalanceCents
    }

    /** Saved on a goal after a manual expense taken from that goal. */
    fun goalNetCents(transferCents: Long, expenseCents: Long): Long =
        (transferCents - expenseCents).coerceAtLeast(0L)

    fun activeGoalMetrics(goals: List<com.example.money.domain.model.SavingsGoal>): DashboardGoalMetrics {
        val activeGoals = goals.filter { it.kind == GoalKind.MONEY && !it.isCompleted }
        return DashboardGoalMetrics(
            totalAccumulatedCents = activeGoals.sumOf { it.currentAmountCents },
            totalRemainingToGoalCents = activeGoals.sumOf { it.remainingAmountCents },
        )
    }
}
