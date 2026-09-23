package com.example.money.widget

import android.content.Context
import com.example.money.R
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.domain.model.BudgetSummary
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.repository.BudgetRepository
import com.example.money.domain.repository.DebtorRepository
import com.example.money.domain.repository.GoalRepository
import com.example.money.domain.util.DebtDueDateUtils
import com.example.money.domain.util.DebtMetricsEngine
import com.example.money.domain.util.GlobalFinanceEngine
import com.example.money.domain.util.MoneyMath
import com.example.money.settings.CurrencyRegion
import com.example.money.settings.GamificationState
import com.example.money.settings.PrivacySettings
import com.example.money.ui.util.Formatters
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class GoalWidgetSnapshotWriter(
    private val context: Context,
    private val goalRepository: GoalRepository,
    private val debtorRepository: DebtorRepository,
    private val budgetRepository: BudgetRepository,
    private val pendingTransactionCount: kotlinx.coroutines.flow.Flow<Int>,
    private val preferencesStore: MoneyPreferencesStore,
    private val currencyRegion: kotlinx.coroutines.flow.Flow<CurrencyRegion>,
) {

    fun observeSnapshotChanges() =
        combine(
            combine(
                goalRepository.observeAllGoals(),
                debtorRepository.getActiveDebtors(),
                pendingTransactionCount,
                currencyRegion,
                budgetRepository.observeBudgetSummary(),
            ) { goals, debts, pendingCount, region, budget ->
                SnapshotCoreInputs(goals, debts, pendingCount, region, budget)
            },
            preferencesStore.privacySettings,
            preferencesStore.gamificationState,
        ) { core, privacy, gamification ->
            val goals = core.goals
            val debts = core.debts
            val pendingCount = core.pendingCount
            val region = core.region
            val budget = core.budget
            val now = System.currentTimeMillis()
            val hideWidget = privacy.hideOnWidget || privacy.hideAmounts
            val activeGoals = goals.filter { !it.isCompleted }
            val totalCurrentCents = activeGoals.sumOf { it.currentAmountCents }
            val totalTargetCents = activeGoals.sumOf { it.targetAmountCents }
            val currentAmount = MoneyMath.fromCents(totalCurrentCents)
            val targetAmount = MoneyMath.fromCents(totalTargetCents)
            val fraction = MoneyMath.progressFraction(totalTargetCents, totalCurrentCents)
            val goalMetrics = GlobalFinanceEngine.activeGoalMetrics(goals)
            val debtMetrics = DebtMetricsEngine.computeMetrics(debts)
            val overdueDebtCount = debts.count { !it.isPaid && DebtDueDateUtils.isOverdue(it.dateDue, now) }

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
                activeItems = buildActiveItems(activeGoals, debts, now, hideWidget),
                globalBalance = budget.globalBalance,
                availableBalance = budget.availableBalance,
                totalDeposits = budget.totalDeposits,
                totalGoalAllocated = budget.totalGoalAllocated,
                totalExpenses = budget.totalExpenses,
                totalAccumulated = goalMetrics.totalAccumulated,
                totalRemainingToGoal = goalMetrics.totalRemainingToGoal,
                owedToMeCents = debtMetrics.owedToMeCents,
                owedToMeCount = debtMetrics.owedToMeActiveCount,
                iOweCents = debtMetrics.iOweCents,
                iOweCount = debtMetrics.iOweActiveCount,
                activeGoalsCount = activeGoals.size,
                overdueDebtCount = overdueDebtCount,
                hideAmounts = hideWidget,
                streakCount = gamification.streakCount,
                lastWinMessage = gamification.lastWinMessage,
            )
        }.distinctUntilChanged()

    private data class SnapshotCoreInputs(
        val goals: List<SavingsGoal>,
        val debts: List<Debtor>,
        val pendingCount: Int,
        val region: CurrencyRegion,
        val budget: BudgetSummary,
    )

    suspend fun persist(snapshot: WidgetGoalSnapshot) {
        preferencesStore.writeWidgetSnapshot(snapshot)
    }

    private fun buildActiveItems(
        goals: List<SavingsGoal>,
        debts: List<Debtor>,
        now: Long,
        hide: Boolean,
    ): List<WidgetFinanceItem> {
        val goalItems = goals
            .filter { it.remainingAmountCents > 0L }
            .sortedByDescending { it.remainingAmountCents }
            .map { goal ->
                WidgetFinanceItem(
                    id = goal.id,
                    payKey = "goal-${goal.id}",
                    type = WidgetItemType.GOAL,
                    name = goal.name.ifBlank { context.getString(R.string.widget_goal_fallback) },
                    remainingLabel = Formatters.currencyFromCents(goal.remainingAmountCents, hide = hide),
                    progressFraction = goal.progressFraction,
                )
            }

        val debtItems = debts
            .filter { !it.isPaid && it.remainingAmountCents > 0L }
            .sortedWith(
                compareByDescending<Debtor> { DebtDueDateUtils.isOverdue(it.dateDue, now) }
                    .thenBy { it.dateDue },
            )
            .map { debt ->
                WidgetFinanceItem(
                    id = debt.id,
                    payKey = "debt-${debt.id}",
                    type = WidgetItemType.DEBT,
                    name = debt.name,
                    remainingLabel = Formatters.currencyFromCents(debt.remainingAmountCents, hide = hide),
                    progressFraction = debt.progressFraction,
                    isOverdue = DebtDueDateUtils.isOverdue(debt.dateDue, now),
                )
            }

        return (debtItems + goalItems).take(3)
    }
}
