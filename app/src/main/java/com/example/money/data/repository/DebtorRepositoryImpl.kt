package com.example.money.data.repository

import androidx.room.withTransaction
import com.example.money.data.local.AppDatabase
import com.example.money.data.local.dao.DebtPaymentDao
import com.example.money.data.local.dao.DebtorDao
import com.example.money.data.local.dao.ExpenseDao
import com.example.money.data.local.dao.TransferDao
import com.example.money.data.local.entity.DebtAdvanceEntity
import com.example.money.data.local.entity.DebtPaymentEntity
import com.example.money.data.local.entity.SyncTombstoneEntity
import com.example.money.data.local.entity.ExpenseEntity
import com.example.money.data.local.entity.TransferEntity
import com.example.money.data.mapper.toDomain
import com.example.money.data.mapper.toEntity
import com.example.money.data.sync.DebtAdvanceIds
import com.example.money.data.sync.FinanceSnapshotCodec
import com.example.money.data.sync.SyncQueueRecorder
import com.example.money.data.sync.SyncWriteGuard
import com.example.money.domain.sync.SyncEntityType
import com.example.money.domain.model.DebtStatus
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Expense
import com.example.money.domain.model.Transfer
import com.example.money.domain.repository.DebtorRepository
import com.example.money.domain.util.DebtDueDateUtils
import com.example.money.domain.util.DebtOperationLabels
import com.example.money.domain.util.MoneyMath
import com.example.money.ui.debtors.DebtTab
import com.example.money.ui.debtors.DebtorFilter
import com.example.money.settings.AppMode
import com.example.money.ui.debtors.DebtorSort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class DebtorRepositoryImpl(
    private val database: AppDatabase,
    private val debtorDao: DebtorDao,
    private val debtPaymentDao: DebtPaymentDao,
    private val transferDao: TransferDao,
    private val expenseDao: ExpenseDao,
    private val syncQueueRecorder: SyncQueueRecorder,
    private val appMode: Flow<AppMode>,
) : DebtorRepository {

    private fun <T> scoped(query: (String) -> Flow<T>): Flow<T> =
        appMode.map { it.name }.distinctUntilChanged().flatMapLatest(query)

    override fun getAllDebtors(): Flow<List<com.example.money.domain.model.Debtor>> =
        scoped { mode -> debtorDao.getAllDebtorsWithPayments(mode) }
            .map { rows -> DebtorSort.sort(rows.map { it.toDomain() }) }

    override fun getActiveDebtors(): Flow<List<com.example.money.domain.model.Debtor>> =
        scoped { mode -> debtorDao.getActiveDebtorsWithPayments(mode) }
            .map { rows -> DebtorSort.sort(rows.map { it.toDomain() }) }

    override fun observeFilteredDebtors(
        tab: DebtTab,
        filter: DebtorFilter,
        searchQuery: String,
        now: Long,
    ): Flow<List<com.example.money.domain.model.Debtor>> {
        if (tab == DebtTab.Calendar) {
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }
        val debtType = when (tab) {
            DebtTab.OwesMe -> DebtType.CREDITOR
            DebtTab.IOwe -> DebtType.DEBTOR
            DebtTab.Calendar -> DebtType.DEBTOR
        }
        val searchPattern = searchQuery.trim().let { query ->
            if (query.isBlank()) "%" else "%$query%"
        }
        return scoped { mode ->
            debtorDao.observeFilteredWithPayments(
            mode = mode,
            debtType = debtType.name,
            filter = filter.name,
            searchPattern = searchPattern,
            overdueBefore = DebtDueDateUtils.overdueBeforeMillis(now),
        )
        }.map { rows ->
            DebtorSort.sort(rows.map { it.toDomain() }, now)
        }
    }

    override suspend fun addDebtor(debtor: com.example.money.domain.model.Debtor): Long =
        SyncWriteGuard.withWriteLock {
            val syncAt = System.currentTimeMillis()
            var newId = 0L
            database.withTransaction {
                val stamped = debtor.copy(mode = appMode.first())
                val entity = stamped.toEntity()
                newId = debtorDao.insert(entity)
                val stored = debtorDao.getDebtorById(newId) ?: return@withTransaction
                database.debtAdvanceDao().insert(
                    DebtAdvanceEntity(
                        syncId = DebtAdvanceIds.opening(stored.syncId),
                        debtorId = newId,
                        amountCents = stored.targetAmountCents,
                        currency = stored.currency,
                        givenAt = stored.dateGiven,
                        note = "",
                        mode = stored.mode,
                        updatedAt = stored.dateGiven,
                    ),
                )
                if (stamped.affectsBalance) {
                    recordCreationOperation(stamped.copy(id = newId), syncAt)
                }
            }
            debtorDao.getDebtorById(newId)?.let { syncQueueRecorder.recordDebt(it, syncAt) }
            newId
        }

    override suspend fun updateDebtor(
        debtor: com.example.money.domain.model.Debtor,
        previousAffectsBalance: Boolean,
    ) {
        SyncWriteGuard.withWriteLock {
            val syncAt = System.currentTimeMillis()
            database.withTransaction {
                val existing = debtorDao.getDebtorById(debtor.id) ?: return@withTransaction
                val resolvedStatus = MoneyMath.resolveDebtStatus(
                    debtor.targetAmountCents,
                    debtor.currentAmountCents,
                )
                val updated = debtor.toEntity().copy(
                    syncId = existing.syncId,
                    status = resolvedStatus.name,
                )
                debtorDao.update(updated)
                alignAdvances(updated, syncAt)

                if (previousAffectsBalance && !debtor.affectsBalance) {
                    removeAllLinkedOperations(debtor.id)
                } else if (!previousAffectsBalance && debtor.affectsBalance) {
                    recordCreationOperation(debtor.copy(id = debtor.id, status = resolvedStatus), syncAt)
                } else if (debtor.affectsBalance) {
                    syncCreationOperation(debtor, syncAt)
                }
            }
            debtorDao.getDebtorById(debtor.id)?.let { syncQueueRecorder.recordDebt(it, syncAt) }
        }
    }

    override suspend fun getDebtorById(id: Long): com.example.money.domain.model.Debtor? {
        val entity = debtorDao.getDebtorById(id) ?: return null
        val payments = debtPaymentDao.getPaymentsForDebtor(entity.mode, id)
        val advances = database.debtAdvanceDao().getForDebtor(id)
        return entity.toDomain(payments, advances)
    }

    override suspend fun addDebtAdvance(
        debtId: Long,
        amountCents: Long,
        givenAt: Long,
        note: String,
    ): Boolean = SyncWriteGuard.withWriteLock {
        if (amountCents <= 0L) return@withWriteLock false
        var applied = false
        val recordedAt = System.currentTimeMillis()
        database.withTransaction {
            val debtor = debtorDao.getDebtorById(debtId) ?: return@withTransaction
            database.debtAdvanceDao().insert(
                DebtAdvanceEntity(
                    debtorId = debtId,
                    amountCents = amountCents,
                    currency = debtor.currency,
                    givenAt = givenAt,
                    note = note.trim(),
                    mode = debtor.mode,
                    updatedAt = recordedAt,
                ),
            )
            val newTarget = debtor.targetAmountCents + amountCents
            val updated = debtor.copy(
                targetAmountCents = newTarget,
                status = MoneyMath.resolveDebtStatus(newTarget, debtor.currentAmountCents).name,
            )
            debtorDao.update(updated)
            if (updated.affectsBalance) {
                recordCreationOperation(
                    updated.toDomain().copy(
                        targetAmountCents = amountCents,
                        currentAmountCents = 0L,
                    ),
                    givenAt,
                )
            }
            applied = true
        }
        if (applied) {
            debtorDao.getDebtorById(debtId)?.let { syncQueueRecorder.recordDebt(it, recordedAt) }
        }
        applied
    }

    override suspend fun markAsPaid(id: Long, affectsBalance: Boolean) {
        SyncWriteGuard.withWriteLock {
            val debtor = debtorDao.getDebtorById(id) ?: return@withWriteLock
            val remaining = MoneyMath.remainingCents(debtor.targetAmountCents, debtor.currentAmountCents)
            if (remaining > 0L) {
                processDebtPaymentInternal(id, remaining, affectsBalance)
            }
        }
    }

    override suspend fun processDebtPayment(
        debtId: Long,
        paymentCents: Long,
        affectsBalance: Boolean,
        paidAt: Long,
    ): Boolean = recordDebtPayment(debtId, paymentCents, affectsBalance, paidAt) != null

    override suspend fun recordDebtPayment(
        debtId: Long,
        paymentCents: Long,
        affectsBalance: Boolean,
        paidAt: Long,
    ): Long? = SyncWriteGuard.withWriteLock {
        processDebtPaymentInternal(debtId, paymentCents, affectsBalance, paidAt)
    }

    override suspend fun undoDebtPayment(debtId: Long, paymentId: Long) {
        SyncWriteGuard.withWriteLock {
            reversePaymentInternal(debtId, paymentId)
        }
    }

    private suspend fun processDebtPaymentInternal(
        debtId: Long,
        paymentCents: Long,
        affectsBalance: Boolean,
        paidAt: Long = System.currentTimeMillis(),
    ): Long? {
        if (paymentCents <= 0L) return null

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
            val recordedAt = System.currentTimeMillis()

            val insertedPaymentId = debtPaymentDao.insert(
                DebtPaymentEntity(
                    debtorId = debtId,
                    amountCents = cappedPayment,
                    currency = debtor.currency,
                    paidAt = paidAt,
                    affectsBalance = affectsBalance,
                    mode = debtor.mode,
                ),
            )
            paymentId = insertedPaymentId

            val updatedDebtor = debtor.copy(
                currentAmountCents = newCurrent,
                status = newStatus.name,
            )
            debtorDao.update(updatedDebtor)

            if (affectsBalance) {
                recordPaymentOperation(
                    debtor = updatedDebtor,
                    paymentCents = cappedPayment,
                    paymentId = insertedPaymentId,
                    paidAt = paidAt,
                )
            }
            applied = true
        }

        if (applied) {
            val recordedAt = System.currentTimeMillis()
            paymentId?.let { id ->
                debtPaymentDao.getById(id)?.let { payment ->
                    syncQueueRecorder.recordDebtPayment(
                        payment,
                        syncUpdatedAt = maxOf(payment.paidAt, recordedAt),
                    )
                }
            }
            debtorDao.getDebtorById(debtId)?.let { debtor ->
                val latestPaidAt = debtPaymentDao.getPaymentsForDebtor(debtor.mode, debtId)
                    .maxOfOrNull { it.paidAt } ?: recordedAt
                syncQueueRecorder.recordDebt(debtor, maxOf(latestPaidAt, recordedAt))
            }
        }
        return if (applied) paymentId else null
    }

    override suspend fun deleteDebtor(debtor: com.example.money.domain.model.Debtor) {
        SyncWriteGuard.withWriteLock {
            val syncAt = System.currentTimeMillis()
            val existing = debtorDao.getDebtorById(debtor.id) ?: return@withWriteLock
            val deletedPayments = debtPaymentDao.getPaymentsForDebtor(existing.mode, debtor.id)
            database.withTransaction {
                removeAllLinkedOperations(debtor.id)
                debtorDao.delete(existing)
            }
            deletedPayments.forEach { payment ->
                syncQueueRecorder.recordDebtPayment(
                    entity = payment,
                    deleted = true,
                    debtorSyncId = existing.syncId,
                    syncUpdatedAt = syncAt,
                )
            }
            syncQueueRecorder.recordDebt(existing, syncAt, deleted = true)
        }
    }

    override suspend fun handleTransferDeleted(transfer: Transfer) {
        SyncWriteGuard.withWriteLock {
            val debtId = transfer.debtId ?: return@withWriteLock
            if (transfer.debtPaymentId != null) {
                reversePaymentInternal(debtId, transfer.debtPaymentId)
            } else {
                val debtor = debtorDao.getDebtorById(debtId) ?: return@withWriteLock
                if (debtor.affectsBalance) {
                    debtorDao.update(debtor.copy(affectsBalance = false))
                    syncQueueRecorder.recordDebt(debtor.copy(affectsBalance = false), System.currentTimeMillis())
                }
            }
        }
    }

    override suspend fun handleExpenseDeleted(expense: Expense) {
        SyncWriteGuard.withWriteLock {
            val debtId = expense.debtId ?: return@withWriteLock
            if (expense.debtPaymentId != null) {
                reversePaymentInternal(debtId, expense.debtPaymentId)
            } else {
                val debtor = debtorDao.getDebtorById(debtId) ?: return@withWriteLock
                if (debtor.affectsBalance) {
                    debtorDao.update(debtor.copy(affectsBalance = false))
                    syncQueueRecorder.recordDebt(debtor.copy(affectsBalance = false), System.currentTimeMillis())
                }
            }
        }
    }

    private suspend fun reversePaymentInternal(debtId: Long, paymentId: Long) {
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

    private suspend fun alignAdvances(
        debtor: com.example.money.data.local.entity.DebtorEntity,
        at: Long,
    ) {
        val dao = database.debtAdvanceDao()
        val existing = dao.getForDebtor(debtor.id).toMutableList()
        existing.filter { it.mode != debtor.mode }.forEach { advance ->
            val updated = advance.copy(mode = debtor.mode, updatedAt = at)
            dao.update(updated)
            val index = existing.indexOfFirst { it.id == advance.id }
            if (index >= 0) existing[index] = updated
        }
        val sum = existing.sumOf { it.amountCents }
        val diff = debtor.targetAmountCents - sum
        if (diff == 0L) return
        if (existing.isEmpty()) {
            dao.insert(
                DebtAdvanceEntity(
                    syncId = DebtAdvanceIds.opening(debtor.syncId),
                    debtorId = debtor.id,
                    amountCents = debtor.targetAmountCents,
                    currency = debtor.currency,
                    givenAt = debtor.dateGiven,
                    note = "",
                    mode = debtor.mode,
                    updatedAt = at,
                ),
            )
            return
        }
        if (diff > 0L) {
            dao.insert(
                DebtAdvanceEntity(
                    debtorId = debtor.id,
                    amountCents = diff,
                    currency = debtor.currency,
                    givenAt = at,
                    note = "",
                    mode = debtor.mode,
                    updatedAt = at,
                ),
            )
            return
        }
        var cut = -diff
        val ordered = existing.sortedWith(
            compareByDescending<DebtAdvanceEntity> { it.givenAt }.thenByDescending { it.id },
        )
        for (advance in ordered) {
            if (cut <= 0L) break
            val nextAmount = (advance.amountCents - cut).coerceAtLeast(0L)
            cut -= advance.amountCents - nextAmount
            if (nextAmount == 0L) {
                dao.deleteById(advance.id)
                tombstoneAdvance(advance, debtor.syncId, at)
            } else {
                dao.update(advance.copy(amountCents = nextAmount, updatedAt = at))
            }
        }
    }

    private suspend fun tombstoneAdvance(
        advance: DebtAdvanceEntity,
        debtorSyncId: String,
        at: Long,
    ) {
        val snapshot = FinanceSnapshotCodec.toSnapshotDebtAdvance(advance, debtorSyncId).copy(
            deleted = true,
            syncUpdatedAt = at,
        )
        database.syncTombstoneDao().upsert(
            SyncTombstoneEntity(
                entityType = SyncEntityType.DEBT_ADVANCE.name,
                entityId = advance.syncId,
                payloadJson = FinanceSnapshotCodec.encodeDebtAdvance(snapshot),
                syncUpdatedAt = at,
            ),
        )
    }

    private suspend fun recordCreationOperation(debtor: com.example.money.domain.model.Debtor, at: Long) {
        val bookedCents = if (debtor.currentAmountCents > 0L) {
            debtor.remainingAmountCents
        } else {
            debtor.targetAmountCents
        }
        if (bookedCents <= 0L) return
        val amountCents = bookedCents
        when (debtor.debtType) {
            DebtType.CREDITOR -> {
                val id = expenseDao.insert(
                    ExpenseEntity(
                        amountCents = amountCents,
                        category = DebtOperationLabels.categoryDebt,
                        date = at,
                        note = DebtOperationLabels.creationNote(debtor.name),
                        isConfirmed = true,
                        debtId = debtor.id,
                        mode = debtor.mode.name,
                    ),
                )
                expenseDao.getById(id)?.let { syncQueueRecorder.recordExpense(it) }
            }
            DebtType.DEBTOR -> {
                val id = transferDao.insert(
                    TransferEntity(
                        amountCents = amountCents,
                        date = at,
                        isConfirmed = true,
                        goalId = null,
                        debtId = debtor.id,
                        note = DebtOperationLabels.creationNote(debtor.name),
                        mode = debtor.mode.name,
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
        val amountCents = paymentCents
        when (runCatching { DebtType.valueOf(debtor.debtType) }.getOrDefault(DebtType.CREDITOR)) {
            DebtType.CREDITOR -> {
                val id = transferDao.insert(
                    TransferEntity(
                        amountCents = amountCents,
                        date = paidAt,
                        isConfirmed = true,
                        goalId = null,
                        debtId = debtor.id,
                        debtPaymentId = paymentId,
                        note = DebtOperationLabels.returnNote(debtor.name),
                        mode = debtor.mode,
                    ),
                )
                transferDao.getById(id)?.let { syncQueueRecorder.recordTransfer(it) }
            }
            DebtType.DEBTOR -> {
                val id = expenseDao.insert(
                    ExpenseEntity(
                        amountCents = amountCents,
                        category = DebtOperationLabels.categoryDebt,
                        date = paidAt,
                        note = DebtOperationLabels.paymentNote(debtor.name),
                        isConfirmed = true,
                        debtId = debtor.id,
                        debtPaymentId = paymentId,
                        mode = debtor.mode,
                    ),
                )
                expenseDao.getById(id)?.let { syncQueueRecorder.recordExpense(it) }
            }
        }
    }
}
