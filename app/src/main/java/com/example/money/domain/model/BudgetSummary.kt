package com.example.money.domain.model

data class BudgetSummary(
    val totalDeposits: Double,
    val totalGoalAllocated: Double,
    val totalExpenses: Double,
) {
    val totalAccumulated: Double
        get() = totalGoalAllocated

    /** Income − expenses. */
    val globalBalance: Double
        get() = totalDeposits - totalExpenses

    val availableBalance: Double
        get() = totalDeposits - totalGoalAllocated - totalExpenses

    /** Legacy alias used in older UI copy. */
    val totalTransfers: Double
        get() = totalDeposits + totalGoalAllocated

    val balance: Double
        get() = availableBalance
}

data class DashboardGoalMetrics(
    val totalAccumulated: Double,
    val totalRemainingToGoal: Double,
)
