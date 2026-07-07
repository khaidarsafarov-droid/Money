package com.example.money.domain.util

import com.example.money.domain.model.Debtor
import kotlin.math.ceil
import kotlin.math.max

object DebtExtraPaymentEngine {

    private const val MS_PER_DAY = 86_400_000L
    private const val MS_PER_MONTH = 30L * MS_PER_DAY

    data class ExtraPaymentImpact(
        val daysSaved: Int,
        val interestSavedCents: Long,
        val hint: String,
    )

    fun estimateMonthlyPaymentForDebt(debtor: Debtor, now: Long = System.currentTimeMillis()): Long {
        val remaining = debtor.remainingAmountCents
        if (remaining <= 0L) return 0L
        if (debtor.dateDue > now) {
            val monthsUntilDue = max(1.0, (debtor.dateDue - now).toDouble() / MS_PER_MONTH)
            return ceil(remaining / monthsUntilDue).toLong()
        }
        return max(500L, ceil(remaining / 12.0).toLong())
    }

    fun calculateImpact(
        debtor: Debtor,
        extraPaymentCents: Long,
        now: Long = System.currentTimeMillis(),
    ): ExtraPaymentImpact {
        if (debtor.remainingAmountCents <= 0L || extraPaymentCents <= 0L) {
            return ExtraPaymentImpact(0, 0L, "")
        }

        val baseMonthly = max(100L, estimateMonthlyPaymentForDebt(debtor, now))
        val boostedMonthly = baseMonthly + extraPaymentCents
        val remaining = debtor.remainingAmountCents

        val baseMonths = ceil(remaining.toDouble() / baseMonthly).toInt()
        val boostedMonths = ceil(remaining.toDouble() / boostedMonthly).toInt()
        val monthsSaved = max(0, baseMonths - boostedMonths)
        val daysSaved = monthsSaved * 30

        val monthlyRate = debtor.interestRateBps / 10_000.0 / 12.0
        val interestSavedCents = if (monthlyRate > 0) {
            (remaining * monthlyRate * monthsSaved).toLong()
        } else {
            0L
        }

        val extra = extraPaymentCents / 100
        val hint = when {
            daysSaved <= 0 -> "Добавьте $$extra сверх платежа — это ускорит закрытие долга."
            interestSavedCents > 0 -> {
                val saved = interestSavedCents / 100
                "+$$extra сэкономит ~$$saved на процентах и закроет долг на $daysSaved дн. раньше."
            }
            else -> "+$$extra закроет долг на $daysSaved дн. раньше."
        }

        return ExtraPaymentImpact(daysSaved, interestSavedCents, hint)
    }
}
