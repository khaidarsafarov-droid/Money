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

    /** Free wallet. Goal money lives in [totalGoalAllocated] and is not part of this balance. */
    val availableBalance: Double
        get() = totalDeposits - totalExpenses
}
