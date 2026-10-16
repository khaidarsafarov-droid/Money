package com.example.money.domain.model

data class BudgetSummary(
    val totalDeposits: Double,
    val totalGoalAllocated: Double,
    val totalExpenses: Double,
) {
    val totalAccumulated: Double
        get() = totalGoalAllocated

    /** Deposits minus wallet expenses. Goal allocations stay visible in [totalGoalAllocated]. */
    val globalBalance: Double
        get() = totalDeposits - totalExpenses

    /** Free wallet: deposits minus net goal transfers minus wallet expenses. */
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
