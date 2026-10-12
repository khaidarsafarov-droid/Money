package com.example.money.data.sync

import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.data.local.AppDatabase
import com.example.money.data.local.entity.SyncTombstoneEntity
import com.example.money.domain.repository.FinanceSnapshotRepository
import com.example.money.domain.sync.FinanceSnapshot
import com.example.money.domain.sync.SnapshotVoiceSettings
import com.example.money.domain.sync.SyncEngine
import com.example.money.domain.sync.SyncEntityType
import kotlinx.coroutines.flow.first

class FinanceSnapshotRepositoryImpl(
    private val database: AppDatabase,
    private val preferencesStore: MoneyPreferencesStore,
    private val deviceIdProvider: DeviceIdProvider,
) : FinanceSnapshotRepository {

    override suspend fun buildSnapshot(): FinanceSnapshot {
        val deviceId = deviceIdProvider.getDeviceId()
        val tombstones = database.syncTombstoneDao().getAll()
        val liveGoals = database.savingsGoalDao().getAllGoalsOnce()
            .map(FinanceSnapshotCodec::toSnapshotGoal)
        val goals = SyncEngine.mergeRecords(
            liveGoals,
            decodeTombstones(tombstones, SyncEntityType.GOAL, FinanceSnapshotCodec::decodeGoal),
        ) { it.id }
        val debtors = database.debtorDao().getAllDebtorsOnce()
        val payments = database.debtPaymentDao().getAllPaymentsOnce()
        val debtorSyncIds = debtors.associate { it.id to it.syncId }
        val paymentSyncIds = payments.associate { it.id to it.syncId }
        val paymentsByDebtor = payments.groupBy { it.debtorId }
        val liveDebts = debtors.map { debtor ->
            val latestPayment = paymentsByDebtor[debtor.id]?.maxOfOrNull { it.paidAt }
            val syncUpdatedAt = maxOf(debtor.dateGiven, latestPayment ?: debtor.dateGiven)
            FinanceSnapshotCodec.toSnapshotDebt(debtor, syncUpdatedAt)
        }
        val debts = SyncEngine.mergeRecords(
            liveDebts,
            decodeTombstones(tombstones, SyncEntityType.DEBT, FinanceSnapshotCodec::decodeDebt),
        ) { it.id }
        val liveDebtPayments = payments.mapNotNull { payment ->
            val debtorSyncId = debtorSyncIds[payment.debtorId] ?: return@mapNotNull null
            FinanceSnapshotCodec.toSnapshotDebtPayment(payment, debtorSyncId)
        }
        val debtPayments = SyncEngine.mergeRecords(
            liveDebtPayments,
            decodeTombstones(
                tombstones,
                SyncEntityType.DEBT_PAYMENT,
                FinanceSnapshotCodec::decodeDebtPayment,
            ),
        ) { it.id }
        val transferTransactions = database.transferDao().getAllOnce()
            .map { transfer ->
                FinanceSnapshotCodec.toSnapshotTransactionFromTransfer(
                    transfer,
                    debtSyncId = transfer.debtId?.let { debtorSyncIds[it] },
                    debtPaymentSyncId = transfer.debtPaymentId?.let { paymentSyncIds[it] },
                )
            }
        val expenseTransactions = database.expenseDao().getAllOnce()
            .map { expense ->
                FinanceSnapshotCodec.toSnapshotTransactionFromExpense(
                    expense,
                    debtSyncId = expense.debtId?.let { debtorSyncIds[it] },
                    debtPaymentSyncId = expense.debtPaymentId?.let { paymentSyncIds[it] },
                )
            }
        val transactions = SyncEngine.mergeRecords(
            transferTransactions + expenseTransactions,
            decodeTombstones(
                tombstones,
                SyncEntityType.TRANSACTION,
                FinanceSnapshotCodec::decodeTransaction,
            ),
        ) { it.id }
        val voiceSettings = buildVoiceSettings()
        val goalSyncIds = database.savingsGoalDao().getAllGoalsOnce().associate { it.id to it.syncId }
        val liveGoalActions = database.goalActionDao().getAllOnce().mapNotNull { action ->
            val goalSyncId = goalSyncIds[action.goalId] ?: return@mapNotNull null
            FinanceSnapshotCodec.toSnapshotGoalAction(action, goalSyncId)
        }
        val goalActions = SyncEngine.mergeRecords(
            liveGoalActions,
            decodeTombstones(
                tombstones,
                SyncEntityType.GOAL_ACTION,
                FinanceSnapshotCodec::decodeGoalAction,
            ),
        ) { it.id }
        val noteEntities = database.financeNoteDao().getAllNotesOnce()
        val liveNotes = noteEntities.map(FinanceSnapshotCodec::toSnapshotNote)
        val notes = SyncEngine.mergeRecords(
            liveNotes,
            decodeTombstones(tombstones, SyncEntityType.NOTE, FinanceSnapshotCodec::decodeNote),
        ) { it.id }
        val noteSyncIds = noteEntities.associate { it.id to it.syncId }
        val liveNoteLines = database.financeNoteDao().getAllLinesOnce().mapNotNull { line ->
            val noteSyncId = noteSyncIds[line.noteId] ?: return@mapNotNull null
            FinanceSnapshotCodec.toSnapshotNoteLine(line, noteSyncId)
        }
        val noteLines = SyncEngine.mergeRecords(
            liveNoteLines,
            decodeTombstones(
                tombstones,
                SyncEntityType.NOTE_LINE,
                FinanceSnapshotCodec::decodeNoteLine,
            ),
        ) { it.id }

        val updatedAt = listOfNotNull(
            goals.maxOfOrNull { it.syncUpdatedAt },
            debts.maxOfOrNull { it.syncUpdatedAt },
            debtPayments.maxOfOrNull { it.syncUpdatedAt },
            transactions.maxOfOrNull { it.syncUpdatedAt },
            goalActions.maxOfOrNull { it.syncUpdatedAt },
            notes.maxOfOrNull { it.syncUpdatedAt },
            noteLines.maxOfOrNull { it.syncUpdatedAt },
            voiceSettings?.syncUpdatedAt,
        ).maxOrNull() ?: System.currentTimeMillis()

        return FinanceSnapshot(
            schemaVersion = FinanceSnapshot.CURRENT_SCHEMA_VERSION,
            deviceId = deviceId,
            updatedAt = updatedAt,
            goals = goals,
            debts = debts,
            debtPayments = debtPayments,
            transactions = transactions,
            voiceSettings = voiceSettings,
            goalActions = goalActions,
            notes = notes,
            noteLines = noteLines,
        )
    }

    override fun encodeSnapshot(snapshot: FinanceSnapshot): String =
        FinanceSnapshotCodec.encode(snapshot)

    override fun decodeSnapshot(raw: String): FinanceSnapshot =
        FinanceSnapshotCodec.decode(raw)

    private fun <T> decodeTombstones(
        tombstones: List<SyncTombstoneEntity>,
        entityType: SyncEntityType,
        decode: (String) -> T,
    ): List<T> = tombstones
        .asSequence()
        .filter { it.entityType == entityType.name }
        .map { decode(it.payloadJson) }
        .toList()

    private suspend fun buildVoiceSettings(): SnapshotVoiceSettings? {
        val settings = preferencesStore.voiceReportSettings.first()
        val syncUpdatedAt = preferencesStore.getVoiceSettingsSyncUpdatedAt() ?: return null
        return SnapshotVoiceSettings(
            enabled = settings.enabled,
            reportHour = settings.reportHour,
            reportMinute = settings.reportMinute,
            language = settings.language.name,
            gender = settings.gender.name,
            forceInSilentMode = settings.forceInSilentMode,
            lastVoiceReportDate = settings.lastVoiceReportDate,
            lastAppOpenDate = settings.lastAppOpenDate,
            syncUpdatedAt = syncUpdatedAt,
        )
    }
}
