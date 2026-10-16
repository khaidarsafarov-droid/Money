package com.example.money.data.backup

import androidx.room.withTransaction
import com.example.money.data.local.AppDatabase
import com.example.money.data.local.entity.DebtPaymentEntity
import com.example.money.data.local.entity.DebtorEntity
import com.example.money.data.local.entity.ExpenseEntity
import com.example.money.data.local.entity.FinanceNoteEntity
import com.example.money.data.local.entity.FinanceNoteLineEntity
import com.example.money.data.local.entity.GoalActionEntity
import com.example.money.data.local.entity.LuckDiaryEntryEntity
import com.example.money.data.local.entity.SavingsGoalEntity
import com.example.money.data.local.entity.SyncQueueEntity
import com.example.money.data.local.entity.SyncTombstoneEntity
import com.example.money.data.local.entity.TransferEntity
import com.example.money.data.sync.FinanceSnapshotCodec
import com.example.money.data.sync.SyncWriteGuard
import com.example.money.domain.backup.BackupBundle
import com.example.money.domain.backup.BackupValidationException
import com.example.money.domain.util.MoneyMath
import com.example.money.domain.sync.SyncEntityType

class BackupRoomApplier(
    private val database: AppDatabase,
) {

    suspend fun apply(bundle: BackupBundle) {
        SyncWriteGuard.withWriteLock { applyLocked(bundle) }
    }

    internal suspend fun applyLocked(bundle: BackupBundle) {
        SyncWriteGuard.runWithoutRecording {
            database.withTransaction {
                val prior = capturePriorSyncState()
                clearBusinessTables()
                val goalLocalIds = insertGoals(bundle)
                val debtorLocalIds = insertDebtors(bundle)
                val paymentLocalIds = insertDebtPayments(bundle, debtorLocalIds)
                insertTransfers(bundle, goalLocalIds, debtorLocalIds, paymentLocalIds)
                insertExpenses(bundle, goalLocalIds, debtorLocalIds, paymentLocalIds)
                insertLuckDiaryEntries(bundle)
                insertGoalActions(bundle, goalLocalIds)
                val noteLocalIds = insertNotes(bundle)
                insertNoteLines(bundle, noteLocalIds)
                database.syncQueueDao().clearAll()
                database.syncTombstoneDao().clearAll()
                writeRestoreTombstones(bundle, prior)
            }
        }
    }

    private suspend fun capturePriorSyncState(): PriorSyncState =
        PriorSyncState(
            goals = database.savingsGoalDao().getAllGoalsOnce(),
            debtors = database.debtorDao().getAllDebtorsOnce(),
            debtPayments = database.debtPaymentDao().getAllPaymentsOnce(),
            transfers = database.transferDao().getAllOnce(),
            expenses = database.expenseDao().getAllOnce(),
            goalActions = database.goalActionDao().getAllOnce(),
            notes = database.financeNoteDao().getAllNotesOnce(),
            noteLines = database.financeNoteDao().getAllLinesOnce(),
            tombstones = database.syncTombstoneDao().getAll(),
        )

    private suspend fun writeRestoreTombstones(bundle: BackupBundle, prior: PriorSyncState) {
        val now = System.currentTimeMillis()
        val goalIds = bundle.goals.map { it.syncId }.toSet()
        val debtorIds = bundle.debtors.map { it.syncId }.toSet()
        val paymentIds = bundle.debtPayments.map { it.syncId }.toSet()
        val transactionIds = bundle.transfers.map { it.syncId }.toSet() +
            bundle.expenses.map { it.syncId }.toSet()
        val actionIds = bundle.goalActions.map { it.syncId }.toSet()
        val noteIds = bundle.notes.map { it.syncId }.toSet()
        val lineIds = bundle.noteLines.map { it.syncId }.toSet()
        val goalSyncByLocal = prior.goals.associate { it.id to it.syncId }
        val debtorSyncByLocal = prior.debtors.associate { it.id to it.syncId }
        val paymentSyncByLocal = prior.debtPayments.associate { it.id to it.syncId }
        val noteSyncByLocal = prior.notes.associate { it.id to it.syncId }

        prior.goals.filter { it.syncId !in goalIds }.forEach { goal ->
            upsertDeleted(
                SyncEntityType.GOAL,
                goal.syncId,
                FinanceSnapshotCodec.encodeGoal(goal.copy(updatedAt = now), deleted = true),
                now,
            )
        }
        prior.debtors.filter { it.syncId !in debtorIds }.forEach { debtor ->
            upsertDeleted(
                SyncEntityType.DEBT,
                debtor.syncId,
                FinanceSnapshotCodec.encodeDebt(debtor, now, deleted = true),
                now,
            )
        }
        prior.debtPayments.filter { it.syncId !in paymentIds }.forEach { payment ->
            val debtorSyncId = debtorSyncByLocal[payment.debtorId] ?: return@forEach
            upsertDeleted(
                SyncEntityType.DEBT_PAYMENT,
                payment.syncId,
                FinanceSnapshotCodec.encodeDebtPayment(
                    entity = payment,
                    debtorSyncId = debtorSyncId,
                    syncUpdatedAt = now,
                    deleted = true,
                ),
                now,
            )
        }
        prior.transfers.filter { it.syncId !in transactionIds }.forEach { transfer ->
            upsertDeleted(
                SyncEntityType.TRANSACTION,
                transfer.syncId,
                FinanceSnapshotCodec.encodeTransactionFromTransfer(
                    entity = transfer,
                    debtSyncId = transfer.debtId?.let { debtorSyncByLocal[it] },
                    debtPaymentSyncId = transfer.debtPaymentId?.let { paymentSyncByLocal[it] },
                    syncUpdatedAt = now,
                    deleted = true,
                ),
                now,
            )
        }
        prior.expenses.filter { it.syncId !in transactionIds }.forEach { expense ->
            upsertDeleted(
                SyncEntityType.TRANSACTION,
                expense.syncId,
                FinanceSnapshotCodec.encodeTransactionFromExpense(
                    entity = expense,
                    debtSyncId = expense.debtId?.let { debtorSyncByLocal[it] },
                    debtPaymentSyncId = expense.debtPaymentId?.let { paymentSyncByLocal[it] },
                    syncUpdatedAt = now,
                    deleted = true,
                ),
                now,
            )
        }
        prior.goalActions.filter { it.syncId !in actionIds }.forEach { action ->
            val goalSyncId = goalSyncByLocal[action.goalId] ?: return@forEach
            upsertDeleted(
                SyncEntityType.GOAL_ACTION,
                action.syncId,
                FinanceSnapshotCodec.encodeGoalAction(
                    action.copy(updatedAt = now),
                    goalSyncId,
                    deleted = true,
                ),
                now,
            )
        }
        prior.notes.filter { it.syncId !in noteIds }.forEach { note ->
            upsertDeleted(
                SyncEntityType.NOTE,
                note.syncId,
                FinanceSnapshotCodec.encodeNote(note.copy(updatedAt = now), deleted = true),
                now,
            )
        }
        prior.noteLines.filter { it.syncId !in lineIds }.forEach { line ->
            val noteSyncId = noteSyncByLocal[line.noteId] ?: return@forEach
            upsertDeleted(
                SyncEntityType.NOTE_LINE,
                line.syncId,
                FinanceSnapshotCodec.encodeNoteLine(
                    line.copy(updatedAt = now),
                    noteSyncId,
                    deleted = true,
                ),
                now,
            )
        }
        prior.tombstones.forEach { tombstone ->
            val absent = when (tombstone.entityType) {
                SyncEntityType.GOAL.name -> tombstone.entityId !in goalIds
                SyncEntityType.DEBT.name -> tombstone.entityId !in debtorIds
                SyncEntityType.DEBT_PAYMENT.name -> tombstone.entityId !in paymentIds
                SyncEntityType.TRANSACTION.name -> tombstone.entityId !in transactionIds
                SyncEntityType.GOAL_ACTION.name -> tombstone.entityId !in actionIds
                SyncEntityType.NOTE.name -> tombstone.entityId !in noteIds
                SyncEntityType.NOTE_LINE.name -> tombstone.entityId !in lineIds
                else -> true
            }
            if (absent) {
                database.syncTombstoneDao().upsertIfNewer(tombstone)
            }
        }
    }

    private suspend fun upsertDeleted(
        entityType: SyncEntityType,
        entityId: String,
        payloadJson: String,
        syncUpdatedAt: Long,
    ) {
        database.syncTombstoneDao().upsertIfNewer(
            SyncTombstoneEntity(
                entityType = entityType.name,
                entityId = entityId,
                payloadJson = payloadJson,
                syncUpdatedAt = syncUpdatedAt,
            ),
        )
    }

    private data class PriorSyncState(
        val goals: List<SavingsGoalEntity>,
        val debtors: List<DebtorEntity>,
        val debtPayments: List<DebtPaymentEntity>,
        val transfers: List<TransferEntity>,
        val expenses: List<ExpenseEntity>,
        val goalActions: List<GoalActionEntity>,
        val notes: List<FinanceNoteEntity>,
        val noteLines: List<FinanceNoteLineEntity>,
        val tombstones: List<SyncTombstoneEntity>,
    )

    internal suspend fun captureRollbackLocked(): RoomRollbackSnapshot =
        RoomRollbackSnapshot(
            goals = database.savingsGoalDao().getAllGoalsOnce(),
            debtors = database.debtorDao().getAllDebtorsOnce(),
            debtPayments = database.debtPaymentDao().getAllPaymentsOnce(),
            transfers = database.transferDao().getAllOnce(),
            expenses = database.expenseDao().getAllOnce(),
            luckDiaryEntries = database.luckDiaryDao().getAllOnce(),
            goalActions = database.goalActionDao().getAllOnce(),
            notes = database.financeNoteDao().getAllNotesOnce(),
            noteLines = database.financeNoteDao().getAllLinesOnce(),
            syncQueue = database.syncQueueDao().getAllPending(),
            syncTombstones = database.syncTombstoneDao().getAll(),
        )

    internal suspend fun restoreRollbackLocked(snapshot: RoomRollbackSnapshot) {
        SyncWriteGuard.runWithoutRecording {
            database.withTransaction {
                clearBusinessTables()
                snapshot.goals.forEach { database.savingsGoalDao().insert(it) }
                snapshot.debtors.forEach { database.debtorDao().insert(it) }
                snapshot.debtPayments.forEach { database.debtPaymentDao().insert(it) }
                snapshot.transfers.forEach { database.transferDao().insert(it) }
                snapshot.expenses.forEach { database.expenseDao().insert(it) }
                snapshot.luckDiaryEntries.forEach { database.luckDiaryDao().insert(it) }
                snapshot.goalActions.forEach { database.goalActionDao().insert(it) }
                snapshot.notes.forEach { database.financeNoteDao().insertNote(it) }
                snapshot.noteLines.forEach { database.financeNoteDao().insertLine(it) }
                database.syncQueueDao().clearAll()
                snapshot.syncQueue.forEach { database.syncQueueDao().insert(it) }
                database.syncTombstoneDao().clearAll()
                snapshot.syncTombstones.forEach { database.syncTombstoneDao().upsert(it) }
            }
        }
    }

    private suspend fun clearBusinessTables() {
        database.financeNoteDao().clearLines()
        database.financeNoteDao().clearNotes()
        database.goalActionDao().clearAll()
        database.transferDao().clearAll()
        database.expenseDao().clearAll()
        database.debtPaymentDao().clearAll()
        database.luckDiaryDao().clearAll()
        database.savingsGoalDao().clearAll()
        database.debtorDao().clearAll()
    }

    private suspend fun insertGoals(bundle: BackupBundle): Map<String, Long> =
        buildMap {
            bundle.goals.forEach { goal ->
                val localId = database.savingsGoalDao().insert(
                    SavingsGoalEntity(
                        syncId = goal.syncId,
                        name = goal.name,
                        targetAmountCents = goal.targetAmountCents,
                        currentAmountCents = goal.currentAmountCents,
                        currency = goal.currency,
                        updatedAt = goal.updatedAtEpochMillis,
                        deadlineMillis = goal.deadlineEpochMillis,
                        kind = goal.kind.ifBlank { "MONEY" },
                    ),
                )
                put(goal.syncId, localId)
            }
        }

    private suspend fun insertGoalActions(
        bundle: BackupBundle,
        goalLocalIds: Map<String, Long>,
    ) {
        bundle.goalActions.forEach { action ->
            database.goalActionDao().insert(
                GoalActionEntity(
                    goalId = goalLocalIds.requireReference(
                        action.goalSyncId,
                        "Goal action ${action.syncId}",
                        "goal",
                    ),
                    title = action.title,
                    completed = action.completed,
                    sortOrder = action.sortOrder,
                    syncId = action.syncId,
                    updatedAt = action.updatedAtEpochMillis,
                ),
            )
        }
    }

    private suspend fun insertNotes(bundle: BackupBundle): Map<String, Long> =
        buildMap {
            bundle.notes.forEach { note ->
                val localId = database.financeNoteDao().insertNote(
                    FinanceNoteEntity(
                        syncId = note.syncId,
                        title = note.title,
                        kind = note.kind,
                        body = note.body,
                        mark = note.mark,
                        closed = note.closed,
                        createdAt = note.createdAtEpochMillis,
                        updatedAt = note.updatedAtEpochMillis,
                    ),
                )
                put(note.syncId, localId)
            }
        }

    private suspend fun insertNoteLines(
        bundle: BackupBundle,
        noteLocalIds: Map<String, Long>,
    ) {
        bundle.noteLines.forEach { line ->
            database.financeNoteDao().insertLine(
                FinanceNoteLineEntity(
                    noteId = noteLocalIds.requireReference(
                        line.noteSyncId,
                        "Note line ${line.syncId}",
                        "note",
                    ),
                    label = line.label,
                    amountCents = line.amountCents,
                    sortOrder = line.sortOrder,
                    syncId = line.syncId,
                    updatedAt = line.updatedAtEpochMillis,
                ),
            )
        }
    }

    private suspend fun insertDebtors(bundle: BackupBundle): Map<String, Long> =
        buildMap {
            bundle.debtors.forEach { debtor ->
                val localId = database.debtorDao().insert(
                    DebtorEntity(
                        syncId = debtor.syncId,
                        name = debtor.name,
                        targetAmountCents = debtor.targetAmountCents,
                        currentAmountCents = debtor.currentAmountCents,
                        currency = debtor.currency,
                        status = debtor.status,
                        debtType = debtor.debtType,
                        affectsBalance = debtor.affectsBalance,
                        note = debtor.note,
                        dateGiven = debtor.dateGivenEpochMillis,
                        dateDue = debtor.dateDueEpochMillis,
                        loanCategory = debtor.loanCategory,
                        interestRateBps = debtor.interestRateBps,
                        calendarEventId = null,
                        calendarSyncFailed = false,
                        contactLookupKey = null,
                        contactUri = null,
                        contactPhotoUri = null,
                        phone = debtor.phone,
                        email = debtor.email,
                    ),
                )
                put(debtor.syncId, localId)
            }
        }

    private suspend fun insertDebtPayments(
        bundle: BackupBundle,
        debtorLocalIds: Map<String, Long>,
    ): Map<String, Long> = buildMap {
        bundle.debtPayments.forEach { payment ->
            val localId = database.debtPaymentDao().insert(
                DebtPaymentEntity(
                    syncId = payment.syncId,
                    debtorId = debtorLocalIds.requireReference(
                        payment.debtorSyncId,
                        "Debt payment ${payment.syncId}",
                        "debtor",
                    ),
                    amountCents = payment.amountCents,
                    currency = payment.currency,
                    paidAt = payment.paidAtEpochMillis,
                    affectsBalance = payment.affectsBalance,
                ),
            )
            put(payment.syncId, localId)
        }
    }

    private suspend fun insertTransfers(
        bundle: BackupBundle,
        goalLocalIds: Map<String, Long>,
        debtorLocalIds: Map<String, Long>,
        paymentLocalIds: Map<String, Long>,
    ) {
        bundle.transfers.forEach { transfer ->
            database.transferDao().insert(
                TransferEntity(
                    syncId = transfer.syncId,
                    amountCents = MoneyMath.toCents(transfer.amount),
                    date = transfer.dateEpochMillis,
                    isConfirmed = transfer.isConfirmed,
                    goalId = transfer.goalSyncId?.let { syncId ->
                        goalLocalIds.requireReference(syncId, "Transfer ${transfer.syncId}", "goal")
                    },
                    goalSyncId = transfer.goalSyncId,
                    debtId = transfer.debtorSyncId?.let { syncId ->
                        debtorLocalIds.requireReference(
                            syncId,
                            "Transfer ${transfer.syncId}",
                            "debtor",
                        )
                    },
                    debtPaymentId = transfer.debtPaymentSyncId?.let { syncId ->
                        paymentLocalIds.requireReference(
                            syncId,
                            "Transfer ${transfer.syncId}",
                            "debt payment",
                        )
                    },
                    note = transfer.note,
                ),
            )
        }
    }

    private suspend fun insertExpenses(
        bundle: BackupBundle,
        goalLocalIds: Map<String, Long>,
        debtorLocalIds: Map<String, Long>,
        paymentLocalIds: Map<String, Long>,
    ) {
        bundle.expenses.forEach { expense ->
            database.expenseDao().insert(
                ExpenseEntity(
                    syncId = expense.syncId,
                    amountCents = MoneyMath.toCents(expense.amount),
                    category = expense.category,
                    date = expense.dateEpochMillis,
                    note = expense.note,
                    isConfirmed = expense.isConfirmed,
                    debtId = expense.debtorSyncId?.let { syncId ->
                        debtorLocalIds.requireReference(
                            syncId,
                            "Expense ${expense.syncId}",
                            "debtor",
                        )
                    },
                    debtPaymentId = expense.debtPaymentSyncId?.let { syncId ->
                        paymentLocalIds.requireReference(
                            syncId,
                            "Expense ${expense.syncId}",
                            "debt payment",
                        )
                    },
                    goalId = expense.goalSyncId?.let { syncId ->
                        goalLocalIds.requireReference(syncId, "Expense ${expense.syncId}", "goal")
                    },
                    goalSyncId = expense.goalSyncId,
                ),
            )
        }
    }

    private suspend fun insertLuckDiaryEntries(bundle: BackupBundle) {
        bundle.luckDiaryEntries.forEach { entry ->
            database.luckDiaryDao().insert(
                LuckDiaryEntryEntity(
                    syncId = entry.syncId,
                    entryDate = entry.entryDateEpochMillis,
                    note = entry.note,
                    amountCents = entry.amountCents,
                    category = entry.category,
                    createdAt = entry.createdAtEpochMillis,
                ),
            )
        }
    }

    private fun Map<String, Long>.requireReference(
        syncId: String,
        owner: String,
        target: String,
    ): Long = this[syncId]
        ?: throw BackupValidationException("$owner references an unknown $target sync ID")

    internal data class RoomRollbackSnapshot(
        val goals: List<SavingsGoalEntity>,
        val debtors: List<DebtorEntity>,
        val debtPayments: List<DebtPaymentEntity>,
        val transfers: List<TransferEntity>,
        val expenses: List<ExpenseEntity>,
        val luckDiaryEntries: List<LuckDiaryEntryEntity>,
        val goalActions: List<GoalActionEntity>,
        val notes: List<FinanceNoteEntity>,
        val noteLines: List<FinanceNoteLineEntity>,
        val syncQueue: List<SyncQueueEntity>,
        val syncTombstones: List<SyncTombstoneEntity>,
    )
}
