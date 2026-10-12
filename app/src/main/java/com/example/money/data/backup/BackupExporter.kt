package com.example.money.data.backup

import com.example.money.data.datastore.PortablePreferencesStorage
import com.example.money.data.local.AppDatabase
import com.example.money.data.sync.SyncWriteGuard
import com.example.money.domain.backup.BackupBundle
import com.example.money.domain.backup.BackupDebtPayment
import com.example.money.domain.backup.BackupDebtor
import com.example.money.domain.backup.BackupExpense
import com.example.money.domain.backup.BackupGoal
import com.example.money.domain.backup.BackupGoalAction
import com.example.money.domain.backup.BackupLuckDiaryEntry
import com.example.money.domain.backup.BackupNote
import com.example.money.domain.backup.BackupNoteLine
import com.example.money.domain.backup.BackupManifest
import com.example.money.domain.backup.BackupRecordCounts
import com.example.money.domain.backup.BackupTransfer
import com.example.money.domain.backup.BackupValidationException

class BackupExporter(
    private val database: AppDatabase,
    private val preferencesStore: PortablePreferencesStorage,
    private val visionImageStorage: VisionImageStorage,
    private val appVersionName: String,
    private val codec: BackupBundleCodec = BackupBundleCodec(),
    private val crypto: PasswordBackupCrypto = PasswordBackupCrypto(),
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {

    suspend fun exportBundle(): BackupBundle =
        SyncWriteGuard.withWriteLock { exportBundleLocked() }

    suspend fun exportEncrypted(password: CharArray): ByteArray =
        SyncWriteGuard.withWriteLock {
            crypto.encrypt(codec.encode(exportBundleLocked()), password)
        }

    internal suspend fun exportBundleLocked(): BackupBundle {
        val goals = database.savingsGoalDao().getAllGoalsOnce()
        val debtors = database.debtorDao().getAllDebtorsOnce()
        val debtPayments = database.debtPaymentDao().getAllPaymentsOnce()
        val transfers = database.transferDao().getAllOnce()
        val expenses = database.expenseDao().getAllOnce()
        val luckEntries = database.luckDiaryDao().getAllOnce()
        val goalActions = database.goalActionDao().getAllOnce()
        val notes = database.financeNoteDao().getAllNotesOnce()
        val noteLines = database.financeNoteDao().getAllLinesOnce()
        val goalSyncIds = goals.associate { it.id to it.syncId }
        val noteSyncIds = notes.associate { it.id to it.syncId }
        val debtorSyncIds = debtors.associate { it.id to it.syncId }
        val paymentSyncIds = debtPayments.associate { it.id to it.syncId }

        return codec.canonicalize(
            BackupBundle(
                manifest = BackupManifest(
                    formatVersion = BackupBundle.CURRENT_FORMAT_VERSION,
                    createdAtEpochMillis = currentTimeMillis(),
                    appVersionName = appVersionName,
                    recordCounts = BackupRecordCounts.EMPTY,
                    contentSha256 = "",
                ),
                goals = goals.map { goal ->
                    BackupGoal(
                        syncId = goal.syncId,
                        name = goal.name,
                        targetAmountCents = goal.targetAmountCents,
                        currentAmountCents = goal.currentAmountCents,
                        currency = goal.currency,
                        updatedAtEpochMillis = goal.updatedAt,
                        deadlineEpochMillis = goal.deadlineMillis,
                        kind = goal.kind,
                    )
                },
                goalActions = goalActions.map { action ->
                    BackupGoalAction(
                        syncId = action.syncId,
                        goalSyncId = goalSyncIds.requireReference(
                            action.goalId,
                            "Goal action ${action.syncId}",
                            "goal",
                        ),
                        title = action.title,
                        completed = action.completed,
                        sortOrder = action.sortOrder,
                        updatedAtEpochMillis = action.updatedAt,
                    )
                },
                notes = notes.map { note ->
                    BackupNote(
                        syncId = note.syncId,
                        title = note.title,
                        kind = note.kind,
                        body = note.body,
                        mark = note.mark,
                        closed = note.closed,
                        createdAtEpochMillis = note.createdAt,
                        updatedAtEpochMillis = note.updatedAt,
                    )
                },
                noteLines = noteLines.map { line ->
                    BackupNoteLine(
                        syncId = line.syncId,
                        noteSyncId = noteSyncIds.requireReference(
                            line.noteId,
                            "Note line ${line.syncId}",
                            "note",
                        ),
                        label = line.label,
                        amountCents = line.amountCents,
                        sortOrder = line.sortOrder,
                        updatedAtEpochMillis = line.updatedAt,
                    )
                },
                debtors = debtors.map { debtor ->
                    BackupDebtor(
                        syncId = debtor.syncId,
                        name = debtor.name,
                        targetAmountCents = debtor.targetAmountCents,
                        currentAmountCents = debtor.currentAmountCents,
                        currency = debtor.currency,
                        status = debtor.status,
                        debtType = debtor.debtType,
                        affectsBalance = debtor.affectsBalance,
                        note = debtor.note,
                        dateGivenEpochMillis = debtor.dateGiven,
                        dateDueEpochMillis = debtor.dateDue,
                        loanCategory = debtor.loanCategory,
                        interestRateBps = debtor.interestRateBps,
                        phone = debtor.phone,
                        email = debtor.email,
                    )
                },
                debtPayments = debtPayments.map { payment ->
                    BackupDebtPayment(
                        syncId = payment.syncId,
                        debtorSyncId = debtorSyncIds.requireReference(
                            payment.debtorId,
                            "Debt payment ${payment.syncId}",
                            "debtor",
                        ),
                        amountCents = payment.amountCents,
                        currency = payment.currency,
                        paidAtEpochMillis = payment.paidAt,
                        affectsBalance = payment.affectsBalance,
                    )
                },
                transfers = transfers.map { transfer ->
                    BackupTransfer(
                        syncId = transfer.syncId,
                        amount = transfer.amount,
                        dateEpochMillis = transfer.date,
                        isConfirmed = transfer.isConfirmed,
                        goalSyncId = transfer.goalId?.let { goalId ->
                            goalSyncIds.requireReference(goalId, "Transfer ${transfer.syncId}", "goal")
                        },
                        debtorSyncId = transfer.debtId?.let { debtorId ->
                            debtorSyncIds.requireReference(
                                debtorId,
                                "Transfer ${transfer.syncId}",
                                "debtor",
                            )
                        },
                        debtPaymentSyncId = transfer.debtPaymentId?.let { paymentId ->
                            paymentSyncIds.requireReference(
                                paymentId,
                                "Transfer ${transfer.syncId}",
                                "debt payment",
                            )
                        },
                        note = transfer.note,
                    )
                },
                expenses = expenses.map { expense ->
                    BackupExpense(
                        syncId = expense.syncId,
                        amount = expense.amount,
                        category = expense.category,
                        dateEpochMillis = expense.date,
                        note = expense.note,
                        isConfirmed = expense.isConfirmed,
                        debtorSyncId = expense.debtId?.let { debtorId ->
                            debtorSyncIds.requireReference(
                                debtorId,
                                "Expense ${expense.syncId}",
                                "debtor",
                            )
                        },
                        debtPaymentSyncId = expense.debtPaymentId?.let { paymentId ->
                            paymentSyncIds.requireReference(
                                paymentId,
                                "Expense ${expense.syncId}",
                                "debt payment",
                            )
                        },
                        goalSyncId = expense.goalId?.let { goalId ->
                            goalSyncIds.requireReference(goalId, "Expense ${expense.syncId}", "goal")
                        },
                    )
                },
                luckDiaryEntries = luckEntries.map { entry ->
                    BackupLuckDiaryEntry(
                        syncId = entry.syncId,
                        entryDateEpochMillis = entry.entryDate,
                        note = entry.note,
                        amountCents = entry.amountCents,
                        category = entry.category,
                        createdAtEpochMillis = entry.createdAt,
                    )
                },
                preferences = preferencesStore.exportPortable(),
                visionImage = visionImageStorage.readCurrent(),
            ),
        )
    }

    private fun Map<Long, String>.requireReference(
        localId: Long,
        owner: String,
        target: String,
    ): String = this[localId]
        ?: throw BackupValidationException("$owner references a missing local $target")
}
