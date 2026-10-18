package com.example.money.domain.model

data class BudgetSummary(
    val totalDepositsCents: Long,
    val totalGoalAllocatedCents: Long,
    val totalExpensesCents: Long,
) {
    val totalAccumulatedCents: Long
        get() = totalGoalAllocatedCents

    /** Deposits minus wallet expenses. Goal savings stay on each goal. */
    val globalBalanceCents: Long
        get() = totalDepositsCents - totalExpensesCents

    /** Free wallet only. A goal's own balance does not reduce this figure. */
    val availableBalanceCents: Long
        get() = globalBalanceCents

    /** Legacy alias used in older UI copy. */
    val totalTransfersCents: Long
        get() = totalDepositsCents + totalGoalAllocatedCents

    val balanceCents: Long
        get() = availableBalanceCents
}

data class DashboardGoalMetrics(
    val totalAccumulatedCents: Long,
    val totalRemainingToGoalCents: Long,
)
