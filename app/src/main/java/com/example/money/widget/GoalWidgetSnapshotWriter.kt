package com.example.money.widget

import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.repository.DebtorRepository
import com.example.money.domain.repository.GoalRepository
import com.example.money.settings.CurrencyRegion
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class GoalWidgetSnapshotWriter(
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
        ) { core, privacy ->
            val region = core.region
            val hideWidget = privacy.hideOnWidget || privacy.hideAmounts
            buildWidgetGoalSnapshot(
                goals = core.goals,
                debts = core.debts,
                pendingCount = core.pendingCount,
                region = region,
                hideAmounts = hideWidget,
                streakCount = 0,
                lastWinMessage = null,
                now = System.currentTimeMillis(),
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
