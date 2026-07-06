package com.example.money.widget

import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.repository.BudgetRepository
import com.example.money.domain.repository.DebtorRepository
import com.example.money.domain.repository.GoalRepository
import com.example.money.domain.util.DebtMetricsEngine
import com.example.money.domain.util.GlobalFinanceEngine
import com.example.money.domain.util.MoneyMath
import com.example.money.settings.CurrencyRegion
import com.example.money.ui.util.Formatters
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Записывает подтверждённый прогресс из локальной Room БД в DataStore.
 * Полностью офлайн — без сети и облака.
 */
class GoalWidgetSnapshotWriter(
    private val goalRepository: GoalRepository,
    private val debtorRepository: DebtorRepository,
    private val budgetRepository: BudgetRepository,
    private val pendingTransactionCount: kotlinx.coroutines.flow.Flow<Int>,
    private val preferencesStore: MoneyPreferencesStore,
    private val currencyRegion: kotlinx.coroutines.flow.Flow<CurrencyRegion>,
) {

    fun observeSnapshotChanges() =
        combine(
            goalRepository.observeAllGoals(),
            debtorRepository.getActiveDebtors(),
            pendingTransactionCount,
            currencyRegion,
            budgetRepository.observeBudgetSummary(),
        ) { goals, debts, pendingCount, region, budget ->
            val now = System.currentTimeMillis()
            val activeGoals = goals.filter { !it.isCompleted }
            val totalCurrentCents = activeGoals.sumOf { it.currentAmountCents }
            val totalTargetCents = activeGoals.sumOf { it.targetAmountCents }
            val currentAmount = MoneyMath.fromCents(totalCurrentCents)
            val targetAmount = MoneyMath.fromCents(totalTargetCents)
            val fraction = MoneyMath.progressFraction(totalTargetCents, totalCurrentCents)
            val goalMetrics = GlobalFinanceEngine.activeGoalMetrics(goals)
            val debtMetrics = DebtMetricsEngine.computeMetrics(debts)
            val overdueDebtCount = debts.count { !it.isPaid && it.dateDue < now }

            WidgetGoalSnapshot(
                currentAmount = currentAmount,
                targetAmount = targetAmount,
                progressFraction = fraction,
                progressPercent = (fraction * 100).toInt(),
                remainingAmount = MoneyMath.fromCents(
                    MoneyMath.remainingCents(totalTargetCents, totalCurrentCents),
                ),
                pendingCount = pendingCount,
                updatedAt = now,
                currencyRegion = region,
                activeItems = buildActiveItems(activeGoals, debts, now),
                globalBalance = budget.globalBalance,
                totalDeposits = budget.totalDeposits,
                totalExpenses = budget.totalExpenses,
                totalAccumulated = goalMetrics.totalAccumulated,
                totalRemainingToGoal = goalMetrics.totalRemainingToGoal,
                owedToMeCents = debtMetrics.owedToMeCents,
                owedToMeCount = debtMetrics.owedToMeActiveCount,
                iOweCents = debtMetrics.iOweCents,
                iOweCount = debtMetrics.iOweActiveCount,
                activeGoalsCount = activeGoals.size,
                overdueDebtCount = overdueDebtCount,
            )
        }.distinctUntilChanged()

    suspend fun persist(snapshot: WidgetGoalSnapshot) {
        preferencesStore.writeWidgetSnapshot(snapshot)
    }

    private fun buildActiveItems(
        goals: List<SavingsGoal>,
        debts: List<Debtor>,
        now: Long,
    ): List<WidgetFinanceItem> {
        val goalItems = goals
            .filter { it.remainingAmountCents > 0L }
            .sortedByDescending { it.remainingAmountCents }
            .map { goal ->
                WidgetFinanceItem(
                    id = goal.id,
                    payKey = "goal-${goal.id}",
                    type = WidgetItemType.GOAL,
                    name = goal.name.ifBlank { "Цель" },
                    remainingLabel = Formatters.currencyFromCents(goal.remainingAmountCents),
                    progressFraction = goal.progressFraction,
                )
            }

        val debtItems = debts
            .filter { !it.isPaid && it.remainingAmountCents > 0L }
            .sortedWith(
                compareByDescending<Debtor> { it.dateDue < now }
                    .thenBy { it.dateDue },
            )
            .map { debt ->
                WidgetFinanceItem(
                    id = debt.id,
                    payKey = "debt-${debt.id}",
                    type = WidgetItemType.DEBT,
                    name = debt.name,
                    remainingLabel = Formatters.currencyFromCents(debt.remainingAmountCents),
                    progressFraction = debt.progressFraction,
                    isOverdue = debt.dateDue < now,
                )
            }

        return (debtItems + goalItems).take(3)
    }
}
