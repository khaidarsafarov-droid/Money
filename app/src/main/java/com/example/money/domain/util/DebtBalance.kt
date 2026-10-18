package com.example.money.domain.util

object DebtBalance {
    /** Remaining principal: money given out minus money returned. Never negative. */
    fun remainingCents(advanceCents: List<Long>, paymentCents: List<Long>): Long {
        val given = advanceCents.sumOf { cents -> cents.coerceAtLeast(0L) }
        val returned = paymentCents.sumOf { cents -> cents.coerceAtLeast(0L) }
        return (given - returned).coerceAtLeast(0L)
    }
}
