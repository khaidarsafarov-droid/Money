package com.example.money.domain.util

import com.example.money.domain.model.DebtPayment
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.DebtType
import kotlin.math.ceil
import kotlin.math.max

object DebtFreedomEngine {

    val milestonePercents = listOf(25, 50, 75, 100)

    private const val MS_PER_DAY = 86_400_000L
    private const val MS_PER_MONTH = 30L * MS_PER_DAY
    private const val PAYMENT_WINDOW_MS = 90L * MS_PER_DAY
    private const val SMALL_DEBT_CENTS = 10_000L
    private const val MIN_MONTHLY_CENTS = 100L
    private const val DEFAULT_ACCELERATE_CENTS = 500L

    data class DebtMilestone(
        val percent: Int,
        val reached: Boolean,
        val centsToReach: Long,
    )

    data class DebtMilestonesResult(
        val progressPercent: Int,
        val milestones: List<DebtMilestone>,
        val nextMilestone: DebtMilestone?,
    )

    data class FreedomProjection(
        val freedomDateMillis: Long?,
        val monthsRemaining: Int?,
        val totalRemainingCents: Long,
        val monthlyPaymentCents: Long,
        val monthsSavedWithExtra: Int?,
        val hasActiveDebts: Boolean,
    )

    fun computeMilestones(currentCents: Long, targetCents: Long): DebtMilestonesResult {
        if (targetCents <= 0L) {
            return DebtMilestonesResult(0, emptyList(), null)
        }

        val progressPercent = ((currentCents.toDouble() / targetCents) * 100).toInt().coerceAtMost(100)
        val milestones = milestonePercents.map { percent ->
            val thresholdCents = ceil(targetCents * percent / 100.0).toLong()
            val reached = currentCents >= thresholdCents
            val centsToReach = max(thresholdCents - currentCents, 0L)
            DebtMilestone(percent, reached, centsToReach)
        }
        val nextMilestone = milestones.firstOrNull { !it.reached }

        return DebtMilestonesResult(progressPercent, milestones, nextMilestone)
    }

    fun suggestAcceleratePaymentCents(
        remainingCents: Long,
        targetCents: Long,
        currentCents: Long,
    ): Long {
        if (remainingCents <= 0L) return 0L

        val milestoneGap = computeMilestones(currentCents, targetCents)
            .nextMilestone
            ?.centsToReach
            ?: remainingCents

        return minOf(
            remainingCents,
            max(DEFAULT_ACCELERATE_CENTS, milestoneGap),
        )
    }

    fun estimatePaymentsToClose(remainingCents: Long, paymentCents: Long): Int {
        if (paymentCents <= 0L || remainingCents <= 0L) return 0
        return ceil(remainingCents.toDouble() / paymentCents).toInt()
    }

    fun isSmallDebt(remainingCents: Long): Boolean =
        remainingCents in 1..SMALL_DEBT_CENTS

    fun calculateFreedomProjection(
        debts: List<Debtor>,
        debtType: DebtType = DebtType.DEBTOR,
        extraMonthlyCents: Long = 0L,
        now: Long = System.currentTimeMillis(),
    ): FreedomProjection {
        val active = debts.filter { !it.isPaid && it.debtType == debtType && it.remainingAmountCents > 0L }
        val totalRemaining = active.sumOf { it.remainingAmountCents }

        if (active.isEmpty()) {
            return FreedomProjection(
                freedomDateMillis = null,
                monthsRemaining = 0,
                totalRemainingCents = 0L,
                monthlyPaymentCents = 0L,
                monthsSavedWithExtra = null,
                hasActiveDebts = false,
            )
        }

        val baseMonthly = estimateMonthlyPaymentCents(active, now)
        val effectiveMonthly = max(MIN_MONTHLY_CENTS, baseMonthly + extraMonthlyCents)
        val monthsRemaining = ceil(totalRemaining.toDouble() / effectiveMonthly).toInt()

        val freedomDate = addMonths(now, monthsRemaining)

        val monthsSavedWithExtra = if (extraMonthlyCents > 0L && baseMonthly > 0L) {
            val baseMonths = ceil(totalRemaining.toDouble() / max(MIN_MONTHLY_CENTS, baseMonthly)).toInt()
            max(0, baseMonths - monthsRemaining)
        } else {
            null
        }

        return FreedomProjection(
            freedomDateMillis = freedomDate,
            monthsRemaining = monthsRemaining,
            totalRemainingCents = totalRemaining,
            monthlyPaymentCents = baseMonthly,
            monthsSavedWithExtra = monthsSavedWithExtra,
            hasActiveDebts = true,
        )
    }

    private fun estimateMonthlyPaymentCents(debts: List<Debtor>, now: Long): Long {
        if (debts.isEmpty()) return 0L

        val recentPayments = debts
            .flatMap { debtor -> debtor.paymentHistory.map { payment -> payment to debtor } }
            .filter { (payment, _) -> payment.paidAt >= now - PAYMENT_WINDOW_MS }

        if (recentPayments.isNotEmpty()) {
            val total = recentPayments.sumOf { (payment, _) -> payment.amountCents }
            val oldest = recentPayments.minOf { (payment, _) -> payment.paidAt }
            val months = max(1.0, (now - oldest).toDouble() / MS_PER_MONTH)
            return ceil(total / months).toLong()
        }

        val totalRemaining = debts.sumOf { it.remainingAmountCents }
        val withDue = debts.filter { it.dateDue > 0L }
        if (withDue.isNotEmpty()) {
            val nearestDue = withDue.minOf { it.dateDue }
            val monthsUntilDue = max(
                1.0,
                (nearestDue - now).toDouble() / MS_PER_MONTH,
            )
            return ceil(totalRemaining / monthsUntilDue).toLong()
        }

        return max(DEFAULT_ACCELERATE_CENTS, ceil(totalRemaining / 12.0).toLong())
    }

    private fun addMonths(fromMillis: Long, months: Int): Long {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = fromMillis
            add(java.util.Calendar.MONTH, months)
        }
        return calendar.timeInMillis
    }

    fun isoDateLabel(millis: Long): String {
        val formatter = java.text.DateFormat.getDateInstance(
            java.text.DateFormat.LONG,
            java.util.Locale.getDefault(),
        )
        return formatter.format(java.util.Date(millis))
    }
}
