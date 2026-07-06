package com.example.money.domain.model

data class GlobalFinanceSummary(
    val totalDeposits: Double,
    val totalGoalAllocated: Double,
    val totalExpenses: Double,
) {
    val totalAccumulated: Double
        get() = totalGoalAllocated

    val globalBalance: Double
        get() = totalDeposits - totalExpenses

    val availableBalance: Double
        get() = totalDeposits - totalGoalAllocated - totalExpenses
}
