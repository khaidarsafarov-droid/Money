package com.example.money.domain.util

import com.example.money.domain.model.DashboardGoalMetrics
import com.example.money.domain.model.GlobalFinanceSummary

object GlobalFinanceEngine {

    fun computeSummary(
        totalDeposits: Double,
        totalGoalAllocated: Double,
        totalExpenses: Double,
    ): GlobalFinanceSummary = GlobalFinanceSummary(
        totalDeposits = totalDeposits,
        totalGoalAllocated = totalGoalAllocated,
        totalExpenses = totalExpenses,
    )

    fun canAfford(summary: GlobalFinanceSummary, amount: Double): Boolean {
        if (amount <= 0.0) return false
        return amount <= summary.availableBalance + 0.000_001
    }

    fun activeGoalMetrics(goals: List<com.example.money.domain.model.SavingsGoal>): DashboardGoalMetrics {
        val activeGoals = goals.filter { !it.isCompleted }
        val accumulated = activeGoals.sumOf { it.currentAmountCents } / 100.0
        val remaining = activeGoals.sumOf { it.remainingAmountCents } / 100.0
        return DashboardGoalMetrics(
            totalAccumulated = accumulated,
            totalRemainingToGoal = remaining,
        )
    }
}
