package com.example.money.domain.model

data class GlobalFinanceSummary(
    val totalDepositsCents: Long,
    val totalGoalAllocatedCents: Long,
    val totalExpensesCents: Long,
) {
    val totalAccumulatedCents: Long
        get() = totalGoalAllocatedCents

    val globalBalanceCents: Long
        get() = totalDepositsCents - totalExpensesCents

    /**
     * Free wallet only: deposits minus expenses that are not tied to a goal.
     * Goal savings are not part of this figure and do not reduce it.
     */
    val availableBalanceCents: Long
        get() = globalBalanceCents
}
