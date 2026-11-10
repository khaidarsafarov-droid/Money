package com.example.money.widget

import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.repository.DebtorRepository
import com.example.money.domain.repository.GoalRepository
import com.example.money.settings.CurrencyRegion
import com.example.money.settings.PrivacySettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * Собирает снимок виджета из целей, долгов и настроек.
 * Запись обратно в DataStore не считается новым событием: иначе свежий
 * [WidgetGoalSnapshot.updatedAt] снова будит те же потоки.
 */
class GoalWidgetSnapshotWriter internal constructor(
    private val goals: Flow<List<SavingsGoal>>,
    private val debts: Flow<List<Debtor>>,
    private val pendingTransactionCount: Flow<Int>,
    private val currencyRegion: Flow<CurrencyRegion>,
    private val privacySettings: Flow<PrivacySettings>,
    private val nowMillis: () -> Long,
    private val readStored: suspend () -> WidgetGoalSnapshot,
    private val writeStored: suspend (WidgetGoalSnapshot) -> Unit,
) {

    constructor(
        goalRepository: GoalRepository,
        debtorRepository: DebtorRepository,
        pendingTransactionCount: Flow<Int>,
        preferencesStore: MoneyPreferencesStore,
        currencyRegion: Flow<CurrencyRegion>,
        nowMillis: () -> Long = System::currentTimeMillis,
    ) : this(
        goals = goalRepository.observeAllGoals(),
        debts = debtorRepository.getAllDebtors(),
        pendingTransactionCount = pendingTransactionCount,
        currencyRegion = currencyRegion,
        privacySettings = preferencesStore.privacySettings,
        nowMillis = nowMillis,
        readStored = { preferencesStore.widgetSnapshot.first() },
        writeStored = { snapshot -> preferencesStore.writeWidgetSnapshot(snapshot) },
    )

    fun observeSnapshotChanges(): Flow<WidgetGoalSnapshot> =
        combine(
            combine(
                goals,
                debts,
                pendingTransactionCount,
                currencyRegion,
            ) { goalList, debtList, pendingCount, region ->
                SnapshotCoreInputs(goalList, debtList, pendingCount, region)
            },
            privacySettings,
        ) { core, privacy ->
            val hideWidget = privacy.hideOnWidget || privacy.hideAmounts
            buildWidgetGoalSnapshot(
                goals = core.goals,
                debts = core.debts,
                pendingCount = core.pendingCount,
                region = core.region,
                hideAmounts = hideWidget,
                streakCount = 0,
                lastWinMessage = null,
                now = nowMillis(),
            )
        }.distinctWidgetContent()

    private data class SnapshotCoreInputs(
        val goals: List<SavingsGoal>,
        val debts: List<Debtor>,
        val pendingCount: Int,
        val region: CurrencyRegion,
    )

    suspend fun persist(snapshot: WidgetGoalSnapshot) {
        if (readStored().contentEquals(snapshot)) return
        writeStored(snapshot)
    }
}
