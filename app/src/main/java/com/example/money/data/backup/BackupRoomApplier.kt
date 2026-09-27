package com.example.money.data.backup

import androidx.room.withTransaction
import com.example.money.data.local.AppDatabase
import com.example.money.data.local.entity.DebtPaymentEntity
import com.example.money.data.local.entity.DebtorEntity
import com.example.money.data.local.entity.ExpenseEntity
import com.example.money.data.local.entity.LuckDiaryEntryEntity
import com.example.money.data.local.entity.SavingsGoalEntity
import com.example.money.data.local.entity.SyncQueueEntity
import com.example.money.data.local.entity.SyncTombstoneEntity
import com.example.money.data.local.entity.TransferEntity
import com.example.money.data.sync.SyncWriteGuard
import com.example.money.domain.backup.BackupBundle
import com.example.money.domain.backup.BackupValidationException

class BackupRoomApplier(
    private val database: AppDatabase,
) {

    suspend fun apply(bundle: BackupBundle) {
        SyncWriteGuard.withWriteLock { applyLocked(bundle) }
    }

    internal suspend fun applyLocked(bundle: BackupBundle) {
        SyncWriteGuard.runWithoutRecording {
            database.withTransaction {
                clearBusinessTables()
                val goalLocalIds = insertGoals(bundle)
                val debtorLocalIds = insertDebtors(bundle)
                val paymentLocalIds = insertDebtPayments(bundle, debtorLocalIds)
                insertTransfers(bundle, goalLocalIds, debtorLocalIds, paymentLocalIds)
                insertExpenses(bundle, goalLocalIds, debtorLocalIds, paymentLocalIds)
                insertLuckDiaryEntries(bundle)
                database.syncQueueDao().clearAll()
                database.syncTombstoneDao().clearAll()
            }
        }
    }

    internal suspend fun captureRollbackLocked(): RoomRollbackSnapshot =
        RoomRollbackSnapshot(
            goals = database.savingsGoalDao().getAllGoalsOnce(),
            debtors = database.debtorDao().getAllDebtorsOnce(),
            debtPayments = database.debtPaymentDao().getAllPaymentsOnce(),
            transfers = database.transferDao().getAllOnce(),
            expenses = database.expenseDao().getAllOnce(),
            luckDiaryEntries = database.luckDiaryDao().getAllOnce(),
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
                database.syncQueueDao().clearAll()
                snapshot.syncQueue.forEach { database.syncQueueDao().insert(it) }
                database.syncTombstoneDao().clearAll()
                snapshot.syncTombstones.forEach { database.syncTombstoneDao().upsert(it) }
            }
        }
    }

    private suspend fun clearBusinessTables() {
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
                    ),
                )
                put(goal.syncId, localId)
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
                    amount = transfer.amount,
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
                    amount = expense.amount,
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
        val syncQueue: List<SyncQueueEntity>,
        val syncTombstones: List<SyncTombstoneEntity>,
    )
}
