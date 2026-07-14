package com.example.money.domain.util

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
        val paceBasedMonths = ceil(totalRemaining.toDouble() / effectiveMonthly).toInt()
        val paceBasedFreedom = addMonths(now, paceBasedMonths)

        val latestDueDate = active
            .filter { it.dateDue > now }
            .maxOfOrNull { it.dateDue }

        val freedomDate = if (latestDueDate != null) {
            minOf(latestDueDate, paceBasedFreedom)
        } else {
            paceBasedFreedom
        }

        val monthsRemaining = monthsUntil(now, freedomDate)

        val monthsSavedWithExtra = if (extraMonthlyCents > 0L && baseMonthly > 0L) {
            val baseEffectiveMonthly = max(MIN_MONTHLY_CENTS, baseMonthly)
            val basePaceMonths = ceil(totalRemaining.toDouble() / baseEffectiveMonthly).toInt()
            val basePaceFreedom = addMonths(now, basePaceMonths)
            val baseFreedomDate = if (latestDueDate != null) {
                minOf(latestDueDate, basePaceFreedom)
            } else {
                basePaceFreedom
            }
            val baseMonthsRemaining = monthsUntil(now, baseFreedomDate)
            max(0, baseMonthsRemaining - monthsRemaining)
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

        val totalRemaining = debts.sumOf { it.remainingAmountCents }
        val withFutureDue = debts.filter { it.dateDue > now }
        if (withFutureDue.isNotEmpty()) {
            val latestDue = withFutureDue.maxOf { it.dateDue }
            val monthsUntilDue = max(
                1.0,
                (latestDue - now).toDouble() / MS_PER_MONTH,
            )
            return ceil(totalRemaining / monthsUntilDue).toLong()
        }

        val recentPayments = debts
            .flatMap { debtor -> debtor.paymentHistory.map { payment -> payment to debtor } }
            .filter { (payment, _) -> payment.paidAt >= now - PAYMENT_WINDOW_MS }

        if (recentPayments.isNotEmpty()) {
            val total = recentPayments.sumOf { (payment, _) -> payment.amountCents }
            val oldest = recentPayments.minOf { (payment, _) -> payment.paidAt }
            val months = max(1.0, (now - oldest).toDouble() / MS_PER_MONTH)
            return ceil(total / months).toLong()
        }

        return max(DEFAULT_ACCELERATE_CENTS, ceil(totalRemaining / 12.0).toLong())
    }

    private fun monthsUntil(fromMillis: Long, toMillis: Long): Int {
        if (toMillis <= fromMillis) return 0
        val days = ceil((toMillis - fromMillis).toDouble() / MS_PER_DAY)
        return max(1, ceil(days / 30.0).toInt())
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
