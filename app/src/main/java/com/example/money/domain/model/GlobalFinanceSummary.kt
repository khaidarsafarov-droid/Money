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

    /** Free wallet: deposits minus net goal transfers minus wallet expenses. */
    val availableBalanceCents: Long
        get() = totalDepositsCents - totalGoalAllocatedCents - totalExpensesCents
}
