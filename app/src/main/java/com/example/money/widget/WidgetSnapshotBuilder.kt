package com.example.money.widget

import com.example.money.domain.model.Debtor
import com.example.money.domain.model.GoalKind
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.util.DebtDueDateUtils
import com.example.money.domain.util.DebtMetricsEngine
import com.example.money.domain.util.GlobalFinanceEngine
import com.example.money.domain.util.MoneyMath
import com.example.money.settings.CurrencyRegion
import com.example.money.ui.debtors.DebtorSort

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
    goalFallbackName: String,
    formatCents: (Long) -> String,
    actionStepsLabel: (done: Int, total: Int) -> String,
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
        activeItems = buildWidgetActiveItems(
            goals = goals,
            debts = debts,
            now = now,
            goalFallbackName = goalFallbackName,
            formatCents = formatCents,
            actionStepsLabel = actionStepsLabel,
        ),
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
    )
}

private fun buildWidgetActiveItems(
    goals: List<SavingsGoal>,
    debts: List<Debtor>,
    now: Long,
    goalFallbackName: String,
    formatCents: (Long) -> String,
    actionStepsLabel: (done: Int, total: Int) -> String,
): List<WidgetFinanceItem> {
    val debtItems = debts
        .filter { !it.isPaid && it.remainingAmountCents > 0L }
        .sortedWith(
            compareBy<Debtor> { DebtorSort.rank(it, now) }
                .thenBy { it.dateDue },
        )
        .map { debt ->
            WidgetFinanceItem(
                id = debt.id,
                payKey = "debt-${debt.id}",
                type = WidgetItemType.DEBT,
                name = debt.name,
                remainingLabel = formatCents(debt.remainingAmountCents),
                progressFraction = debt.progressFraction,
                isOverdue = debt.dateDue > 0L && DebtDueDateUtils.isOverdue(debt.dateDue, now),
            )
        }

    val actionItems = goals
        .filter { it.kind == GoalKind.ACTION && !it.isCompleted }
        .sortedBy { it.actionTotal - it.actionDone }
        .map { goal ->
            WidgetFinanceItem(
                id = goal.id,
                payKey = "goal-${goal.id}",
                type = WidgetItemType.GOAL,
                name = goal.name.ifBlank { goalFallbackName },
                remainingLabel = actionStepsLabel(goal.actionDone, goal.actionTotal),
                progressFraction = goal.actionProgressFraction,
                isOverdue = false,
            )
        }

    val moneyItems = goals
        .filter { it.kind == GoalKind.MONEY && !it.isCompleted && it.remainingAmountCents > 0L }
        .sortedByDescending { it.remainingAmountCents }
        .map { goal ->
            WidgetFinanceItem(
                id = goal.id,
                payKey = "goal-${goal.id}",
                type = WidgetItemType.GOAL,
                name = goal.name.ifBlank { goalFallbackName },
                remainingLabel = formatCents(goal.remainingAmountCents),
                progressFraction = goal.progressFraction,
                isOverdue = false,
            )
        }

    return (debtItems + actionItems + moneyItems).take(3)
}
