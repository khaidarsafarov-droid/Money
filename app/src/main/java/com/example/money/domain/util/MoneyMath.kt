package com.example.money.domain.util

import com.example.money.domain.model.DebtStatus
import kotlin.math.roundToLong

object MoneyMath {

    fun toCents(amount: Double): Long = (amount * 100.0).roundToLong()

    fun fromCents(cents: Long): Double = cents / 100.0

    fun remainingCents(targetCents: Long, currentCents: Long): Long =
        maxOf(targetCents - currentCents, 0L)

    fun progressFraction(targetCents: Long, currentCents: Long): Float =
        GoalProgress.displayProgressFraction(targetCents, currentCents)

    fun resolveDebtStatus(targetCents: Long, currentCents: Long): DebtStatus = when {
        currentCents <= 0L -> DebtStatus.ACTIVE
        currentCents >= targetCents -> DebtStatus.COMPLETED
        else -> DebtStatus.PARTIAL
    }
}
