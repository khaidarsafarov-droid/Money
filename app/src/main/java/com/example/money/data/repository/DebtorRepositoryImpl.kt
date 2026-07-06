package com.example.money.data.repository

import androidx.room.withTransaction
import com.example.money.data.local.AppDatabase
import com.example.money.data.local.dao.DebtPaymentDao
import com.example.money.data.local.dao.DebtorDao
import com.example.money.data.local.dao.ExpenseDao
import com.example.money.data.local.dao.TransferDao
import com.example.money.data.local.entity.DebtPaymentEntity
import com.example.money.data.local.entity.ExpenseEntity
import com.example.money.data.local.entity.TransferEntity
import com.example.money.data.mapper.toDomain
import com.example.money.data.mapper.toEntity
import com.example.money.data.sync.SyncQueueRecorder
import com.example.money.domain.model.DebtStatus
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Expense
import com.example.money.domain.model.Transfer
import com.example.money.domain.repository.DebtorRepository
import com.example.money.domain.util.DebtOperationLabels
import com.example.money.domain.util.MoneyMath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DebtorRepositoryImpl(
    private val database: AppDatabase,
    private val debtorDao: DebtorDao,
    private val debtPaymentDao: DebtPaymentDao,
    private val transferDao: TransferDao,
    private val expenseDao: ExpenseDao,
    private val syncQueueRecorder: SyncQueueRecorder,
) : DebtorRepository {

    override fun getAllDebtors(): Flow<List<com.example.money.domain.model.Debtor>> =
        debtorDao.getAllDebtorsWithPayments().map { rows -> rows.map { it.toDomain() } }

    override fun getActiveDebtors(): Flow<List<com.example.money.domain.model.Debtor>> =
        debtorDao.getActiveDebtorsWithPayments().map { rows -> rows.map { it.toDomain() } }

    override suspend fun addDebtor(debtor: com.example.money.domain.model.Debtor): Long {
        val syncAt = System.currentTimeMillis()
        var newId = 0L
        database.withTransaction {
            newId = debtorDao.insert(debtor.toEntity())
            if (debtor.affectsBalance) {
                recordCreationOperation(debtor.copy(id = newId), syncAt)
            }
        }
        debtorDao.getDebtorById(newId)?.let { syncQueueRecorder.recordDebt(it, syncAt) }
        return newId
    }

    override suspend fun updateDebtor(
        debtor: com.example.money.domain.model.Debtor,
        previousAffectsBalance: Boolean,
    ) {
        val syncAt = System.currentTimeMillis()
        database.withTransaction {
            val existing = debtorDao.getDebtorById(debtor.id) ?: return@withTransaction
            val resolvedStatus = MoneyMath.resolveDebtStatus(
                debtor.targetAmountCents,
                debtor.currentAmountCents,
            )
            val updated = existing.copy(
                name = debtor.name,
                targetAmountCents = debtor.targetAmountCents,
                currentAmountCents = debtor.currentAmountCents,
                currency = debtor.currency,
                status = resolvedStatus.name,
                debtType = debtor.debtType.name,
                affectsBalance = debtor.affectsBalance,
                note = debtor.note,
                dateGiven = debtor.dateGiven,
                dateDue = debtor.dateDue,
            )
            debtorDao.update(updated)

            if (previousAffectsBalance && !debtor.affectsBalance) {
                removeCreationOperations(debtor.id)
            } else if (!previousAffectsBalance && debtor.affectsBalance) {
                recordCreationOperation(debtor.copy(status = resolvedStatus), syncAt)
            } else if (debtor.affectsBalance) {
                syncCreationOperation(debtor, syncAt)
            }
        }
        debtorDao.getDebtorById(debtor.id)?.let { syncQueueRecorder.recordDebt(it, syncAt) }
    }

    override suspend fun getDebtorById(id: Long): com.example.money.domain.model.Debtor? {
        val entity = debtorDao.getDebtorById(id) ?: return null
        val payments = debtPaymentDao.getPaymentsForDebtor(id)
        return entity.toDomain(payments)
    }

    override suspend fun markAsPaid(id: Long, affectsBalance: Boolean) {
        val debtor = debtorDao.getDebtorById(id) ?: return
        val remaining = MoneyMath.remainingCents(debtor.targetAmountCents, debtor.currentAmountCents)
        if (remaining > 0L) {
            processDebtPayment(id, remaining, affectsBalance)
        }
    }

    override suspend fun processDebtPayment(debtId: Long, paymentCents: Long, affectsBalance: Boolean): Boolean {
        if (paymentCents <= 0L) return false

        var applied = false
        var paymentId: Long? = null
        database.withTransaction {
            val debtor = debtorDao.getDebtorById(debtId) ?: return@withTransaction
            if (debtor.status == DebtStatus.COMPLETED.name) return@withTransaction

            val remaining = MoneyMath.remainingCents(debtor.targetAmountCents, debtor.currentAmountCents)
            val cappedPayment = minOf(paymentCents, remaining)
            if (cappedPayment <= 0L) return@withTransaction

            val newCurrent = debtor.currentAmountCents + cappedPayment
            val newStatus = MoneyMath.resolveDebtStatus(debtor.targetAmountCents, newCurrent)
            val syncAt = System.currentTimeMillis()

            paymentId = debtPaymentDao.insert(
                DebtPaymentEntity(
                    debtorId = debtId,
                    amountCents = cappedPayment,
                    currency = debtor.currency,
                    paidAt = syncAt,
                    affectsBalance = affectsBalance,
                ),
            )

            val updatedDebtor = debtor.copy(
                currentAmountCents = newCurrent,
                status = newStatus.name,
            )
            debtorDao.update(updatedDebtor)

            if (affectsBalance) {
                recordPaymentOperation(
                    debtor = updatedDebtor,
                    paymentCents = cappedPayment,
                    paymentId = paymentId!!,
                    paidAt = syncAt,
                )
            }
            applied = true
        }

        if (applied) {
            paymentId?.let { id ->
                debtPaymentDao.getById(id)?.let { syncQueueRecorder.recordDebtPayment(it) }
            }
            debtorDao.getDebtorById(debtId)?.let { debtor ->
                val syncAt = debtPaymentDao.getPaymentsForDebtor(debtId)
                    .maxOfOrNull { it.paidAt } ?: System.currentTimeMillis()
                syncQueueRecorder.recordDebt(debtor, syncAt)
            }
        }
        return applied
    }

    override suspend fun deleteDebtor(debtor: com.example.money.domain.model.Debtor) {
        val syncAt = System.currentTimeMillis()
        val existing = debtorDao.getDebtorById(debtor.id) ?: return
        database.withTransaction {
            removeAllLinkedOperations(debtor.id)
            debtorDao.delete(existing)
        }
        syncQueueRecorder.recordDebt(existing, syncAt, deleted = true)
    }

    override suspend fun handleTransferDeleted(transfer: Transfer) {
        val debtId = transfer.debtId ?: return
        if (transfer.debtPaymentId != null) {
            reversePayment(debtId, transfer.debtPaymentId)
        } else {
            val debtor = debtorDao.getDebtorById(debtId) ?: return
            if (debtor.affectsBalance) {
                debtorDao.update(debtor.copy(affectsBalance = false))
                syncQueueRecorder.recordDebt(debtor.copy(affectsBalance = false), System.currentTimeMillis())
            }
        }
    }

    override suspend fun handleExpenseDeleted(expense: Expense) {
        val debtId = expense.debtId ?: return
        if (expense.debtPaymentId != null) {
            reversePayment(debtId, expense.debtPaymentId)
        } else {
            val debtor = debtorDao.getDebtorById(debtId) ?: return
            if (debtor.affectsBalance) {
                debtorDao.update(debtor.copy(affectsBalance = false))
                syncQueueRecorder.recordDebt(debtor.copy(affectsBalance = false), System.currentTimeMillis())
            }
        }
    }

    private suspend fun reversePayment(debtId: Long, paymentId: Long) {
        val deletedPayment = debtPaymentDao.getById(paymentId)
        database.withTransaction {
            val payment = deletedPayment ?: return@withTransaction
            val debtor = debtorDao.getDebtorById(debtId) ?: return@withTransaction
            val newCurrent = (debtor.currentAmountCents - payment.amountCents).coerceAtLeast(0L)
            val newStatus = MoneyMath.resolveDebtStatus(debtor.targetAmountCents, newCurrent)
            debtorDao.update(
                debtor.copy(
                    currentAmountCents = newCurrent,
                    status = newStatus.name,
                ),
            )
            debtPaymentDao.deleteById(paymentId)
        }
        deletedPayment?.let { syncQueueRecorder.recordDebtPayment(it, deleted = true) }
        debtorDao.getDebtorById(debtId)?.let { syncQueueRecorder.recordDebt(it, System.currentTimeMillis()) }
    }

    private suspend fun recordCreationOperation(debtor: com.example.money.domain.model.Debtor, at: Long) {
        val amount = MoneyMath.fromCents(debtor.targetAmountCents)
        when (debtor.debtType) {
            DebtType.CREDITOR -> {
                val id = expenseDao.insert(
                    ExpenseEntity(
                        amount = amount,
                        category = DebtOperationLabels.categoryDebt,
                        date = at,
                        note = DebtOperationLabels.creationNote(debtor.name),
                        isConfirmed = true,
                        debtId = debtor.id,
                    ),
                )
                expenseDao.getById(id)?.let { syncQueueRecorder.recordExpense(it) }
            }
            DebtType.DEBTOR -> {
                val id = transferDao.insert(
                    TransferEntity(
                        amount = amount,
                        date = at,
                        isConfirmed = true,
                        goalId = null,
                        debtId = debtor.id,
                        note = DebtOperationLabels.creationNote(debtor.name),
                    ),
                )
                transferDao.getById(id)?.let { syncQueueRecorder.recordTransfer(it) }
            }
        }
    }

    private suspend fun syncCreationOperation(debtor: com.example.money.domain.model.Debtor, at: Long) {
        removeCreationOperations(debtor.id)
        recordCreationOperation(debtor, at)
    }

    private suspend fun removeCreationOperations(debtId: Long) {
        transferDao.getByDebtId(debtId)
            .filter { it.debtPaymentId == null }
            .forEach { transfer ->
                transferDao.delete(transfer)
                syncQueueRecorder.recordTransfer(transfer, deleted = true)
            }
        expenseDao.getByDebtId(debtId)
            .filter { it.debtPaymentId == null }
            .forEach { expense ->
                expenseDao.delete(expense)
                syncQueueRecorder.recordExpense(expense, deleted = true)
            }
    }

    private suspend fun removeAllLinkedOperations(debtId: Long) {
        transferDao.getByDebtId(debtId).forEach { transfer ->
            transferDao.delete(transfer)
            syncQueueRecorder.recordTransfer(transfer, deleted = true)
        }
        expenseDao.getByDebtId(debtId).forEach { expense ->
            expenseDao.delete(expense)
            syncQueueRecorder.recordExpense(expense, deleted = true)
        }
    }

    private suspend fun recordPaymentOperation(
        debtor: com.example.money.data.local.entity.DebtorEntity,
        paymentCents: Long,
        paymentId: Long,
        paidAt: Long,
    ) {
        val amount = MoneyMath.fromCents(paymentCents)
        when (DebtType.valueOf(debtor.debtType)) {
            DebtType.CREDITOR -> {
                val id = transferDao.insert(
                    TransferEntity(
                        amount = amount,
                        date = paidAt,
                        isConfirmed = true,
                        goalId = null,
                        debtId = debtor.id,
                        debtPaymentId = paymentId,
                        note = DebtOperationLabels.returnNote(debtor.name),
                    ),
                )
                transferDao.getById(id)?.let { syncQueueRecorder.recordTransfer(it) }
            }
            DebtType.DEBTOR -> {
                val id = expenseDao.insert(
                    ExpenseEntity(
                        amount = amount,
                        category = DebtOperationLabels.categoryDebt,
                        date = paidAt,
                        note = DebtOperationLabels.paymentNote(debtor.name),
                        isConfirmed = true,
                        debtId = debtor.id,
                        debtPaymentId = paymentId,
                    ),
                )
                expenseDao.getById(id)?.let { syncQueueRecorder.recordExpense(it) }
            }
        }
    }
}
