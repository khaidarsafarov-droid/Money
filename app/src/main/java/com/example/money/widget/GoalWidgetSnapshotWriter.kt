package com.example.money.widget

import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.repository.DebtorRepository
import com.example.money.domain.repository.GoalRepository
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
        ) { goals, debts, pendingCount, region ->
            val activeGoals = goals.filter { !it.isCompleted }
            val totalCurrentCents = activeGoals.sumOf { it.currentAmountCents }
            val totalTargetCents = activeGoals.sumOf { it.targetAmountCents }
            val currentAmount = MoneyMath.fromCents(totalCurrentCents)
            val targetAmount = MoneyMath.fromCents(totalTargetCents)
            val fraction = MoneyMath.progressFraction(totalTargetCents, totalCurrentCents)

            WidgetGoalSnapshot(
                currentAmount = currentAmount,
                targetAmount = targetAmount,
                progressFraction = fraction,
                progressPercent = (fraction * 100).toInt(),
                remainingAmount = MoneyMath.fromCents(
                    MoneyMath.remainingCents(totalTargetCents, totalCurrentCents),
                ),
                pendingCount = pendingCount,
                updatedAt = System.currentTimeMillis(),
                currencyRegion = region,
                activeItems = buildActiveItems(activeGoals, debts),
            )
        }.distinctUntilChanged()

    suspend fun persist(snapshot: WidgetGoalSnapshot) {
        preferencesStore.writeWidgetSnapshot(snapshot)
    }

    private fun buildActiveItems(
        goals: List<SavingsGoal>,
        debts: List<Debtor>,
    ): List<WidgetFinanceItem> {
        val goalItems = goals
            .filter { it.remainingAmountCents > 0L }
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

        val debtItems = debts.map { debt ->
            WidgetFinanceItem(
                id = debt.id,
                payKey = "debt-${debt.id}",
                type = WidgetItemType.DEBT,
                name = debt.name,
                remainingLabel = Formatters.currencyFromCents(debt.remainingAmountCents),
                progressFraction = debt.progressFraction,
            )
        }

        return goalItems + debtItems
    }
}
