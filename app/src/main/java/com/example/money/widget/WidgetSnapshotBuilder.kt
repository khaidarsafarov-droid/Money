package com.example.money.widget

import com.example.money.domain.model.Debtor
import com.example.money.domain.model.GoalKind
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.util.DebtDueDateUtils
import com.example.money.domain.util.DebtMetricsEngine
import com.example.money.domain.util.GlobalFinanceEngine
import com.example.money.domain.util.GoalProgress
import com.example.money.domain.util.MoneyMath
import com.example.money.settings.CurrencyRegion

/**
 * Builds the home-screen numbers the widget shows.
 * The percent is goal savings only and is labeled as goals.
 * Debt amounts stay in their own cells and do not move the percent.
 */
internal fun buildWidgetGoalSnapshot(
    goals: List<SavingsGoal>,
    debts: List<Debtor>,
    pendingCount: Int,
    region: CurrencyRegion,
    hideAmounts: Boolean,
    streakCount: Int,
    lastWinMessage: String?,
    now: Long,
): WidgetGoalSnapshot {
    val moneyGoals = goals.filter { it.kind == GoalKind.MONEY && !it.isCompleted }
    val totalCurrentCents = moneyGoals.sumOf { it.currentAmountCents }
    val totalTargetCents = moneyGoals.sumOf { it.targetAmountCents }
    val goalMetrics = GlobalFinanceEngine.activeGoalMetrics(goals)
    val debtMetrics = DebtMetricsEngine.computeMetrics(debts)
    val goalPool = goalMetrics.totalAccumulatedCents + goalMetrics.totalRemainingToGoalCents
    val goalPercent = if (goalPool > 0L) {
        ((goalMetrics.totalAccumulatedCents * 100L) / goalPool).toInt().coerceIn(0, 100)
    } else {
        0
    }
    val scoreUsesDebts = false
    val score = goalPercent
    val overdueDebtCount = debts.count {
        !it.isPaid && it.dateDue > 0L && DebtDueDateUtils.isOverdue(it.dateDue, now)
    }
    val featured = featuredWidgetGoal(goals, now)

    return WidgetGoalSnapshot(
        currentAmount = MoneyMath.fromCents(totalCurrentCents),
        targetAmount = MoneyMath.fromCents(totalTargetCents),
        progressFraction = score / 100f,
        progressPercent = score,
        remainingAmount = MoneyMath.fromCents(
            MoneyMath.remainingCents(totalTargetCents, totalCurrentCents),
        ),
        pendingCount = pendingCount,
        updatedAt = now,
        currencyRegion = region,
        totalAccumulated = MoneyMath.fromCents(goalMetrics.totalAccumulatedCents),
        totalRemainingToGoal = MoneyMath.fromCents(goalMetrics.totalRemainingToGoalCents),
        owedToMeCents = debtMetrics.owedToMeCents,
        owedToMeCount = debtMetrics.owedToMeActiveCount,
        iOweCents = debtMetrics.iOweCents,
        iOweCount = debtMetrics.iOweActiveCount,
        activeGoalsCount = goals.count { !it.isCompleted },
        overdueDebtCount = overdueDebtCount,
        hideAmounts = hideAmounts,
        streakCount = streakCount,
        lastWinMessage = lastWinMessage,
        scoreUsesDebts = scoreUsesDebts,
        featuredGoalName = featured?.name.orEmpty(),
        featuredGoalSavedCents = featured?.let { if (it.kind == GoalKind.ACTION) it.actionDone.toLong() else it.currentAmountCents } ?: 0L,
        featuredGoalTargetCents = featured?.let { if (it.kind == GoalKind.ACTION) it.actionTotal.toLong() else it.targetAmountCents } ?: 0L,
        featuredGoalPercent = featured?.let { if (it.kind == GoalKind.ACTION) it.actionProgressPercent else it.progressPercent } ?: 0,
        featuredGoalIsAction = featured?.kind == GoalKind.ACTION,
    )
}

/** Overdue first, then the soonest deadline, then goals without a date. */
internal fun featuredWidgetGoal(goals: List<SavingsGoal>, now: Long): SavingsGoal? =
    GoalProgress.orderedForSection(goals, now).firstOrNull { !it.isCompleted }
