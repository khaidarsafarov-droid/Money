package com.example.money.data.sync

import androidx.room.withTransaction
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.data.local.AppDatabase
import com.example.money.data.local.entity.DebtPaymentEntity
import com.example.money.data.local.entity.DebtorEntity
import com.example.money.data.local.entity.ExpenseEntity
import com.example.money.data.local.entity.SavingsGoalEntity
import com.example.money.data.local.entity.TransferEntity
import com.example.money.domain.sync.FinanceSnapshot
import com.example.money.domain.sync.SnapshotTransaction
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
                val paymentLocalIds = buildDebtPaymentSyncIdMap(snapshot)
                applyTransactions(snapshot, goalLocalIds, debtorLocalIds, paymentLocalIds)
            }
            snapshot.voiceSettings?.let { preferencesStore.applySyncedVoiceSettings(it) }
        }
    }

    private suspend fun applyGoals(snapshot: FinanceSnapshot) {
        val dao = database.savingsGoalDao()
        val keepIds = snapshot.goals.map { it.id }.toSet()
        dao.getAllGoalsOnce()
            .filter { it.syncId !in keepIds }
            .forEach { dao.delete(it) }
        snapshot.goals.forEach { goal ->
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
        return snapshot.goals.associate { goal ->
            val localId = dao.getBySyncId(goal.id)?.id
                ?: error("Missing local goal for sync id ${goal.id}")
            goal.id to localId
        }
    }

    private suspend fun applyDebts(snapshot: FinanceSnapshot) {
        val dao = database.debtorDao()
        val keepIds = snapshot.debts.map { it.id }.toSet()
        dao.getAllDebtorsOnce()
            .filter { it.syncId !in keepIds }
            .forEach { dao.delete(it) }
        snapshot.debts.forEach { debt ->
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
        return snapshot.debts.associate { debt ->
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
        val keepIds = snapshot.debtPayments.map { it.id }.toSet()
        dao.getAllPaymentsOnce()
            .filter { it.syncId !in keepIds }
            .forEach { dao.deleteById(it.id) }
        snapshot.debtPayments.forEach { payment ->
            val debtorId = debtorLocalIds[payment.debtorId]
                ?: return@forEach
            val existing = dao.getBySyncId(payment.id)
            val entity = DebtPaymentEntity(
                id = existing?.id ?: 0L,
                syncId = payment.id,
                debtorId = debtorId,
                amountCents = payment.amountCents,
                currency = "USD",
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

    private suspend fun buildDebtPaymentSyncIdMap(snapshot: FinanceSnapshot): Map<String, Long> {
        val dao = database.debtPaymentDao()
        return snapshot.debtPayments.associate { payment ->
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
            .filter { it.kind != "EXPENSE" }
            .map { it.id }
            .toSet()
        val keepExpenseIds = snapshot.transactions
            .filter { it.kind == "EXPENSE" }
            .map { it.id }
            .toSet()

        transferDao.getAllOnce()
            .filter { it.syncId !in keepTransferIds }
            .forEach { transferDao.delete(it) }
        expenseDao.getAllOnce()
            .filter { it.syncId !in keepExpenseIds }
            .forEach { expenseDao.delete(it) }

        snapshot.transactions.forEach { transaction ->
            when (transaction.kind) {
                "EXPENSE" -> applyExpenseTransaction(
                    expenseDao,
                    transaction,
                    debtorLocalIds,
                    paymentLocalIds,
                )
                "DEPOSIT", "GOAL_TRANSFER" -> applyTransferTransaction(
                    transferDao,
                    transaction,
                    goalLocalIds,
                    debtorLocalIds,
                    paymentLocalIds,
                )
            }
        }
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
        debtorLocalIds: Map<String, Long>,
        paymentLocalIds: Map<String, Long>,
    ) {
        val existing = expenseDao.getBySyncId(transaction.id)
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
        )
        if (existing == null) {
            expenseDao.insert(entity)
        } else {
            expenseDao.upsert(entity)
        }
    }
}
