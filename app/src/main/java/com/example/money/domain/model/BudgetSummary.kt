package com.example.money.domain.model

data class BudgetSummary(
    val totalDeposits: Double,
    val totalGoalAllocated: Double,
    val totalExpenses: Double,
) {
    val totalAccumulated: Double
        get() = totalGoalAllocated

    /** Free wallet: deposits minus expenses that were not taken from a goal. */
    val globalBalance: Double
        get() = totalDeposits - totalExpenses

    /** Same free wallet. Money placed on a goal is not subtracted here. */
    val availableBalance: Double
        get() = totalDeposits - totalExpenses

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
