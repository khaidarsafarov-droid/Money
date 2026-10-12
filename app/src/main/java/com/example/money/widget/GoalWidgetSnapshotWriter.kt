package com.example.money.widget

import android.content.Context
import com.example.money.R
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.repository.DebtorRepository
import com.example.money.domain.repository.GoalRepository
import com.example.money.settings.CurrencyRegion
import com.example.money.ui.util.Formatters
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class GoalWidgetSnapshotWriter(
    private val context: Context,
    private val goalRepository: GoalRepository,
    private val debtorRepository: DebtorRepository,
    private val pendingTransactionCount: kotlinx.coroutines.flow.Flow<Int>,
    private val preferencesStore: MoneyPreferencesStore,
    private val currencyRegion: kotlinx.coroutines.flow.Flow<CurrencyRegion>,
) {

    fun observeSnapshotChanges() =
        combine(
            combine(
                goalRepository.observeAllGoals(),
                debtorRepository.getAllDebtors(),
                pendingTransactionCount,
                currencyRegion,
            ) { goals, debts, pendingCount, region ->
                SnapshotCoreInputs(goals, debts, pendingCount, region)
            },
            preferencesStore.privacySettings,
            preferencesStore.gamificationState,
        ) { core, privacy, gamification ->
            val region = core.region
            val hideWidget = privacy.hideOnWidget || privacy.hideAmounts
            buildWidgetGoalSnapshot(
                goals = core.goals,
                debts = core.debts,
                pendingCount = core.pendingCount,
                region = region,
                hideAmounts = hideWidget,
                streakCount = gamification.streakCount,
                lastWinMessage = gamification.lastWinMessage,
                now = System.currentTimeMillis(),
                goalFallbackName = context.getString(R.string.widget_goal_fallback),
                formatCents = { cents ->
                    Formatters.currencyFromCents(cents, region, hide = hideWidget)
                },
                actionStepsLabel = { done, total ->
                    context.getString(R.string.goal_action_progress, done, total)
                },
            )
        }.distinctUntilChanged()

    private data class SnapshotCoreInputs(
        val goals: List<SavingsGoal>,
        val debts: List<Debtor>,
        val pendingCount: Int,
        val region: CurrencyRegion,
    )

    suspend fun persist(snapshot: WidgetGoalSnapshot) {
        preferencesStore.writeWidgetSnapshot(snapshot)
    }

}
