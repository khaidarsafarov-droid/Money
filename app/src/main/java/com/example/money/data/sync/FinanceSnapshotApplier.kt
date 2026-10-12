package com.example.money.data.sync

import androidx.room.withTransaction
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.data.local.AppDatabase
import com.example.money.data.local.entity.DebtPaymentEntity
import com.example.money.data.local.entity.DebtorEntity
import com.example.money.data.local.entity.ExpenseEntity
import com.example.money.data.local.entity.FinanceNoteEntity
import com.example.money.data.local.entity.FinanceNoteLineEntity
import com.example.money.data.local.entity.GoalActionEntity
import com.example.money.data.local.entity.SavingsGoalEntity
import com.example.money.data.local.entity.SyncTombstoneEntity
import com.example.money.data.local.entity.TransferEntity
import com.example.money.domain.sync.FinanceSnapshot
import com.example.money.domain.sync.SnapshotGoalAction
import com.example.money.domain.sync.SnapshotNote
import com.example.money.domain.sync.SnapshotNoteLine
import com.example.money.domain.sync.SnapshotTransaction
import com.example.money.domain.sync.SnapshotTransactionBucket
import com.example.money.domain.sync.SyncEntityType
import com.example.money.domain.sync.linkableDebtPayments
import com.example.money.domain.sync.snapshotTransactionBucket
import com.example.money.domain.model.GoalKind
import com.example.money.domain.util.MoneyMath

class FinanceSnapshotApplier(
    private val database: AppDatabase,
    private val preferencesStore: MoneyPreferencesStore,
) {

    suspend fun apply(snapshot: FinanceSnapshot) {
        SyncWriteGuard.runWithoutRecording {
            database.withTransaction {
                applyGoals(snapshot)
                val goalLocalIds = buildGoalSyncIdMap(snapshot)
                applyDebts(snapshot)
                val debtorLocalIds = buildDebtorSyncIdMap(snapshot)
                applyDebtPayments(snapshot, debtorLocalIds)
                val paymentLocalIds = buildDebtPaymentSyncIdMap(snapshot, debtorLocalIds)
                applyTransactions(snapshot, goalLocalIds, debtorLocalIds, paymentLocalIds)
                snapshot.goalActions?.let { applyGoalActions(it, goalLocalIds) }
                val noteLocalIds = snapshot.notes?.let { applyNotes(it) }
                if (noteLocalIds != null && snapshot.noteLines != null) {
                    applyNoteLines(snapshot.noteLines, noteLocalIds)
                }
                applyTombstones(snapshot)
            }
            snapshot.voiceSettings?.let { preferencesStore.applySyncedVoiceSettings(it) }
        }
    }

    private suspend fun applyGoals(snapshot: FinanceSnapshot) {
        val dao = database.savingsGoalDao()
        val liveGoals = snapshot.goals.filterNot { it.deleted }
        val keepIds = liveGoals.map { it.id }.toSet()
        dao.getAllGoalsOnce()
            .filter { it.syncId !in keepIds }
            .forEach { dao.delete(it) }
        liveGoals.forEach { goal ->
            val existing = dao.getBySyncId(goal.id)
            val entity = SavingsGoalEntity(
                id = existing?.id ?: 0L,
                syncId = goal.id,
                name = goal.name,
                targetAmountCents = goal.targetAmountCents,
                currentAmountCents = 0L,
                currency = goal.currency,
                updatedAt = goal.syncUpdatedAt,
                deadlineMillis = goal.dateDeadline?.let(SyncDates::parseDate),
                kind = goal.goalKind ?: existing?.kind ?: GoalKind.MONEY.name,
            )
            if (existing == null) {
                dao.insert(entity)
            } else {
                dao.update(entity)
            }
        }
    }

    private suspend fun buildGoalSyncIdMap(snapshot: FinanceSnapshot): Map<String, Long> {
        val dao = database.savingsGoalDao()
        return snapshot.goals.filterNot { it.deleted }.associate { goal ->
            val localId = dao.getBySyncId(goal.id)?.id
                ?: error("Missing local goal for sync id ${goal.id}")
            goal.id to localId
        }
    }

    private suspend fun applyDebts(snapshot: FinanceSnapshot) {
        val dao = database.debtorDao()
        val liveDebts = snapshot.debts.filterNot { it.deleted }
        val keepIds = liveDebts.map { it.id }.toSet()
        dao.getAllDebtorsOnce()
            .filter { it.syncId !in keepIds }
            .forEach { dao.delete(it) }
        liveDebts.forEach { debt ->
            val existing = dao.getBySyncId(debt.id)
            val entity = DebtorEntity(
                id = existing?.id ?: 0L,
                syncId = debt.id,
                name = debt.name,
                targetAmountCents = debt.targetAmountCents,
                currentAmountCents = debt.currentAmountCents,
                currency = debt.currency,
                status = debt.status,
                debtType = debt.debtType,
                affectsBalance = debt.affectsBalance,
                note = debt.note,
                dateGiven = SyncDates.parseDate(debt.dateGiven),
                dateDue = debt.dateDue?.let(SyncDates::parseDate)
                    ?: SyncDates.parseDate(debt.dateGiven),
                loanCategory = existing?.loanCategory ?: "OTHER",
                interestRateBps = existing?.interestRateBps ?: 0,
                calendarEventId = existing?.calendarEventId,
                calendarSyncFailed = existing?.calendarSyncFailed ?: false,
                contactLookupKey = existing?.contactLookupKey,
                contactUri = existing?.contactUri,
                contactPhotoUri = existing?.contactPhotoUri,
                phone = existing?.phone.orEmpty(),
                email = existing?.email.orEmpty(),
            )
            if (existing == null) {
                dao.insert(entity)
            } else {
                dao.update(entity)
            }
        }
    }

    private suspend fun buildDebtorSyncIdMap(snapshot: FinanceSnapshot): Map<String, Long> {
        val dao = database.debtorDao()
        return snapshot.debts.filterNot { it.deleted }.associate { debt ->
            val localId = dao.getBySyncId(debt.id)?.id
                ?: error("Missing local debtor for sync id ${debt.id}")
            debt.id to localId
        }
    }

    private suspend fun applyDebtPayments(
        snapshot: FinanceSnapshot,
        debtorLocalIds: Map<String, Long>,
    ) {
        val dao = database.debtPaymentDao()
        val linkable = linkableDebtPayments(
            snapshot.debtPayments.filterNot { it.deleted },
            debtorLocalIds.keys,
        )
        val keepIds = linkable.map { it.id }.toSet()
        dao.getAllPaymentsOnce()
            .filter { it.syncId !in keepIds }
            .forEach { dao.deleteById(it.id) }
        linkable.forEach { payment ->
            val debtorId = debtorLocalIds.getValue(payment.debtorId)
            val existing = dao.getBySyncId(payment.id)
            val entity = DebtPaymentEntity(
                id = existing?.id ?: 0L,
                syncId = payment.id,
                debtorId = debtorId,
                amountCents = payment.amountCents,
                currency = payment.currency ?: existing?.currency ?: "USD",
                paidAt = SyncDates.parseDateTime(payment.paidAt),
                affectsBalance = payment.affectsBalance,
            )
            if (existing == null) {
                dao.insert(entity)
            } else {
                dao.upsert(entity)
            }
        }
    }

    private suspend fun buildDebtPaymentSyncIdMap(
        snapshot: FinanceSnapshot,
        debtorLocalIds: Map<String, Long>,
    ): Map<String, Long> {
        val dao = database.debtPaymentDao()
        return linkableDebtPayments(
            snapshot.debtPayments.filterNot { it.deleted },
            debtorLocalIds.keys,
        ).associate { payment ->
            val localId = dao.getBySyncId(payment.id)?.id
                ?: error("Missing local debt payment for sync id ${payment.id}")
            payment.id to localId
        }
    }

    private suspend fun applyTransactions(
        snapshot: FinanceSnapshot,
        goalLocalIds: Map<String, Long>,
        debtorLocalIds: Map<String, Long>,
        paymentLocalIds: Map<String, Long>,
    ) {
        val transferDao = database.transferDao()
        val expenseDao = database.expenseDao()
        val keepTransferIds = snapshot.transactions
            .filter { !it.deleted && snapshotTransactionBucket(it.kind) == SnapshotTransactionBucket.TRANSFER }
            .map { it.id }
            .toSet()
        val keepExpenseIds = snapshot.transactions
            .filter { !it.deleted && snapshotTransactionBucket(it.kind) == SnapshotTransactionBucket.EXPENSE }
            .map { it.id }
            .toSet()

        transferDao.getAllOnce()
            .filter { it.syncId !in keepTransferIds }
            .forEach { transferDao.delete(it) }
        expenseDao.getAllOnce()
            .filter { it.syncId !in keepExpenseIds }
            .forEach { expenseDao.delete(it) }

        snapshot.transactions.filterNot { it.deleted }.forEach { transaction ->
            when (snapshotTransactionBucket(transaction.kind)) {
                SnapshotTransactionBucket.EXPENSE -> applyExpenseTransaction(
                    expenseDao,
                    transaction,
                    goalLocalIds,
                    debtorLocalIds,
                    paymentLocalIds,
                )
                SnapshotTransactionBucket.TRANSFER -> applyTransferTransaction(
                    transferDao,
                    transaction,
                    goalLocalIds,
                    debtorLocalIds,
                    paymentLocalIds,
                )
                SnapshotTransactionBucket.IGNORED -> Unit
            }
        }
    }

    private suspend fun applyTombstones(snapshot: FinanceSnapshot) {
        snapshot.goals.forEach { goal ->
            reconcileTombstone(
                entityType = SyncEntityType.GOAL,
                entityId = goal.id,
                syncUpdatedAt = goal.syncUpdatedAt,
                deleted = goal.deleted,
                payloadJson = FinanceSnapshotCodec.encodeGoal(goal),
            )
        }
        snapshot.debts.forEach { debt ->
            reconcileTombstone(
                entityType = SyncEntityType.DEBT,
                entityId = debt.id,
                syncUpdatedAt = debt.syncUpdatedAt,
                deleted = debt.deleted,
                payloadJson = FinanceSnapshotCodec.encodeDebt(debt),
            )
        }
        snapshot.debtPayments.forEach { payment ->
            reconcileTombstone(
                entityType = SyncEntityType.DEBT_PAYMENT,
                entityId = payment.id,
                syncUpdatedAt = payment.syncUpdatedAt,
                deleted = payment.deleted,
                payloadJson = FinanceSnapshotCodec.encodeDebtPayment(payment),
            )
        }
        snapshot.transactions.forEach { transaction ->
            reconcileTombstone(
                entityType = SyncEntityType.TRANSACTION,
                entityId = transaction.id,
                syncUpdatedAt = transaction.syncUpdatedAt,
                deleted = transaction.deleted,
                payloadJson = FinanceSnapshotCodec.encodeTransaction(transaction),
            )
        }
    }

    private suspend fun reconcileTombstone(
        entityType: SyncEntityType,
        entityId: String,
        syncUpdatedAt: Long,
        deleted: Boolean,
        payloadJson: String,
    ) {
        val dao = database.syncTombstoneDao()
        if (!deleted) {
            dao.deleteIfNotNewer(entityType.name, entityId, syncUpdatedAt)
            return
        }
        dao.upsertIfNewer(
            SyncTombstoneEntity(
                entityType = entityType.name,
                entityId = entityId,
                payloadJson = payloadJson,
                syncUpdatedAt = syncUpdatedAt,
            ),
        )
    }

    private suspend fun applyTransferTransaction(
        transferDao: com.example.money.data.local.dao.TransferDao,
        transaction: SnapshotTransaction,
        goalLocalIds: Map<String, Long>,
        debtorLocalIds: Map<String, Long>,
        paymentLocalIds: Map<String, Long>,
    ) {
        val existing = transferDao.getBySyncId(transaction.id)
        val goalSyncId = transaction.goalId
        val goalId = goalSyncId?.let { goalLocalIds[it] }
        val debtId = transaction.debtId?.let { debtorLocalIds[it] }
        val debtPaymentId = transaction.debtPaymentId?.let { paymentLocalIds[it] }
        val entity = TransferEntity(
            id = existing?.id ?: 0L,
            syncId = transaction.id,
            amount = MoneyMath.fromCents(transaction.amountCents),
            date = SyncDates.parseDateTime(transaction.createdAt),
            isConfirmed = true,
            goalId = goalId,
            goalSyncId = goalSyncId,
            debtId = debtId,
            debtPaymentId = debtPaymentId,
            note = transaction.note.orEmpty(),
        )
        if (existing == null) {
            transferDao.insert(entity)
        } else {
            transferDao.upsert(entity)
        }
    }

    private suspend fun applyExpenseTransaction(
        expenseDao: com.example.money.data.local.dao.ExpenseDao,
        transaction: SnapshotTransaction,
        goalLocalIds: Map<String, Long>,
        debtorLocalIds: Map<String, Long>,
        paymentLocalIds: Map<String, Long>,
    ) {
        val existing = expenseDao.getBySyncId(transaction.id)
        val goalSyncId = transaction.goalId
        val goalId = goalSyncId?.let { goalLocalIds[it] }
        val debtId = transaction.debtId?.let { debtorLocalIds[it] }
        val debtPaymentId = transaction.debtPaymentId?.let { paymentLocalIds[it] }
        val entity = ExpenseEntity(
            id = existing?.id ?: 0L,
            syncId = transaction.id,
            amount = MoneyMath.fromCents(transaction.amountCents),
            category = transaction.category ?: "Other",
            date = SyncDates.parseDateTime(transaction.createdAt),
            note = transaction.note.orEmpty(),
            isConfirmed = true,
            debtId = debtId,
            debtPaymentId = debtPaymentId,
            goalId = goalId,
            goalSyncId = goalSyncId,
        )
        if (existing == null) {
            expenseDao.insert(entity)
        } else {
            expenseDao.upsert(entity)
        }
    }

    private suspend fun applyGoalActions(
        actions: List<SnapshotGoalAction>,
        goalLocalIds: Map<String, Long>,
    ) {
        val dao = database.goalActionDao()
        val live = actions.filter { !it.deleted && goalLocalIds.containsKey(it.goalId) }
        val keepIds = live.map { it.id }.toSet()
        dao.getAllOnce()
            .filter { it.syncId !in keepIds }
            .forEach { dao.deleteById(it.id) }
        live.forEach { action ->
            val goalId = goalLocalIds.getValue(action.goalId)
            val existing = dao.getBySyncId(action.id)
            val entity = GoalActionEntity(
                id = existing?.id ?: 0L,
                goalId = goalId,
                title = action.title,
                completed = action.completed,
                sortOrder = action.sortOrder,
                syncId = action.id,
                updatedAt = action.syncUpdatedAt,
            )
            if (existing == null) {
                dao.insert(entity)
            } else {
                dao.update(entity)
            }
        }
    }

    private suspend fun applyNotes(notes: List<SnapshotNote>): Map<String, Long> {
        val dao = database.financeNoteDao()
        val live = notes.filterNot { it.deleted }
        val keepIds = live.map { it.id }.toSet()
        dao.getAllNotesOnce()
            .filter { it.syncId !in keepIds }
            .forEach { dao.deleteNote(it.id) }
        live.forEach { note ->
            val existing = dao.getNoteBySyncId(note.id)
            val entity = FinanceNoteEntity(
                id = existing?.id ?: 0L,
                title = note.title,
                kind = note.kind,
                body = note.body,
                mark = note.mark,
                closed = note.closed,
                createdAt = note.createdAt,
                updatedAt = note.syncUpdatedAt,
                syncId = note.id,
            )
            if (existing == null) {
                dao.insertNote(entity)
            } else {
                dao.updateNote(entity)
            }
        }
        return live.associate { note ->
            val localId = dao.getNoteBySyncId(note.id)?.id
                ?: error("Missing local note for sync id ${note.id}")
            note.id to localId
        }
    }

    private suspend fun applyNoteLines(
        lines: List<SnapshotNoteLine>,
        noteLocalIds: Map<String, Long>,
    ) {
        val dao = database.financeNoteDao()
        val live = lines.filter { !it.deleted && noteLocalIds.containsKey(it.noteId) }
        val keepIds = live.map { it.id }.toSet()
        dao.getAllLinesOnce()
            .filter { it.syncId !in keepIds }
            .forEach { dao.deleteLine(it.id) }
        live.forEach { line ->
            val noteId = noteLocalIds.getValue(line.noteId)
            val existing = dao.getLineBySyncId(line.id)
            val entity = FinanceNoteLineEntity(
                id = existing?.id ?: 0L,
                noteId = noteId,
                label = line.label,
                amountCents = line.amountCents,
                sortOrder = line.sortOrder,
                syncId = line.id,
                updatedAt = line.syncUpdatedAt,
            )
            if (existing == null) {
                dao.insertLine(entity)
            } else {
                dao.updateLine(entity)
            }
        }
    }
}
