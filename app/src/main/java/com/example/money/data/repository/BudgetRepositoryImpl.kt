package com.example.money.data.repository

import androidx.room.withTransaction
import com.example.money.data.local.AppDatabase
import com.example.money.data.local.dao.ExpenseDao
import com.example.money.data.local.dao.SavingsGoalDao
import com.example.money.data.local.dao.TransferDao
import com.example.money.data.mapper.toDomain
import com.example.money.data.mapper.toEntity
import com.example.money.data.sync.SyncQueueRecorder
import com.example.money.data.sync.SyncWriteGuard
import com.example.money.domain.model.BudgetSummary
import com.example.money.domain.model.CategorySpend
import com.example.money.domain.model.Expense
import com.example.money.domain.model.Transfer
import com.example.money.domain.repository.BudgetRepository
import com.example.money.domain.util.GlobalFinanceEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl(
    private val database: AppDatabase,
    private val transferDao: TransferDao,
    private val expenseDao: ExpenseDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val syncQueueRecorder: SyncQueueRecorder,
) : BudgetRepository {

    override fun getTransfers(): Flow<List<Transfer>> =
        transferDao.getAllTransfers().map { transfers -> transfers.map { it.toDomain() } }

    override fun getExpenses(): Flow<List<Expense>> =
        expenseDao.getAllExpenses().map { expenses -> expenses.map { it.toDomain() } }

    override fun observeBudgetSummary(): Flow<BudgetSummary> =
        combine(
            transferDao.observeDepositTotal(),
            transferDao.observeGoalAllocatedTotal(),
            expenseDao.observeConfirmedTotalAmount(),
        ) { totalDeposits, totalGoalAllocated, totalExpenses ->
            val summary = GlobalFinanceEngine.computeSummary(
                totalDepositsCents = totalDeposits,
                totalGoalAllocatedCents = totalGoalAllocated,
                totalExpensesCents = totalExpenses,
            )
            BudgetSummary(
                totalDepositsCents = summary.totalDepositsCents,
                totalGoalAllocatedCents = summary.totalGoalAllocatedCents,
                totalExpensesCents = summary.totalExpensesCents,
            )
        }

    override fun observeCategorySpend(startMillis: Long, endMillisExclusive: Long): Flow<List<CategorySpend>> =
        expenseDao.observeCategorySpend(startMillis, endMillisExclusive)

    override fun observePendingCount(): Flow<Int> =
        combine(
            transferDao.observePendingCount(),
            expenseDao.observePendingCount(),
        ) { transfers, expenses -> transfers + expenses }

    override suspend fun addDeposit(transfer: Transfer): Long =
        SyncWriteGuard.withWriteLock {
            val id = transferDao.insert(
                transfer.copy(isConfirmed = true, goalId = null).toEntity(),
            )
            transferDao.getById(id)?.let { syncQueueRecorder.recordTransfer(it) }
            id
        }

    override suspend fun addGoalTransfer(transfer: Transfer): Long =
        SyncWriteGuard.withWriteLock {
            val goalSyncId = transfer.goalId?.let { savingsGoalDao.getGoalById(it)?.syncId }
            val id = transferDao.insert(
                transfer.copy(isConfirmed = true).toEntity().copy(goalSyncId = goalSyncId),
            )
            transferDao.getById(id)?.let { syncQueueRecorder.recordTransfer(it) }
            id
        }

    override suspend fun addGoalTransferIfAffordable(transfer: Transfer): Boolean =
        SyncWriteGuard.withWriteLock {
            database.withTransaction {
                if (!canAffordCurrentBalance(transfer.amountCents)) return@withTransaction false
                val goalSyncId = transfer.goalId?.let { savingsGoalDao.getGoalById(it)?.syncId }
                val id = transferDao.insert(
                    transfer.copy(isConfirmed = true).toEntity().copy(goalSyncId = goalSyncId),
                )
                transferDao.getById(id)?.let { syncQueueRecorder.recordTransfer(it) }
                true
            }
        }

    override suspend fun addTransfer(transfer: Transfer): Long = addDeposit(transfer)

    override suspend fun addExpense(expense: Expense): Long =
        SyncWriteGuard.withWriteLock {
            val goalSyncId = expense.goalId?.let { savingsGoalDao.getGoalById(it)?.syncId }
            val id = expenseDao.insert(
                expense.copy(isConfirmed = true).toEntity().copy(goalSyncId = goalSyncId),
            )
            expenseDao.getById(id)?.let { syncQueueRecorder.recordExpense(it) }
            id
        }

    override suspend fun addExpenseIfAffordable(expense: Expense): Boolean =
        SyncWriteGuard.withWriteLock {
            database.withTransaction {
                val goalId = expense.goalId
                if (goalId != null) {
                    if (!canSpendFromGoal(goalId, expense.amountCents)) return@withTransaction false
                } else if (!canAffordCurrentBalance(expense.amountCents)) {
                    return@withTransaction false
                }
                val goalSyncId = goalId?.let { savingsGoalDao.getGoalById(it)?.syncId }
                val id = expenseDao.insert(
                    expense.copy(isConfirmed = true).toEntity().copy(goalSyncId = goalSyncId),
                )
                expenseDao.getById(id)?.let { syncQueueRecorder.recordExpense(it) }
                true
            }
        }

    override suspend fun confirmTransfer(id: Long) {
        SyncWriteGuard.withWriteLock {
            transferDao.confirm(id)
            transferDao.getById(id)?.let { syncQueueRecorder.recordTransfer(it) }
        }
    }

    override suspend fun confirmExpense(id: Long) {
        SyncWriteGuard.withWriteLock {
            expenseDao.confirm(id)
            expenseDao.getById(id)?.let { syncQueueRecorder.recordExpense(it) }
        }
    }

    override suspend fun deleteTransfer(transfer: Transfer) {
        SyncWriteGuard.withWriteLock {
            val existing = transferDao.getById(transfer.id) ?: return@withWriteLock
            transferDao.delete(existing)
            syncQueueRecorder.recordTransfer(existing, deleted = true)
        }
    }

    override suspend fun deleteExpense(expense: Expense) {
        SyncWriteGuard.withWriteLock {
            val existing = expenseDao.getById(expense.id) ?: return@withWriteLock
            expenseDao.delete(existing)
            syncQueueRecorder.recordExpense(existing, deleted = true)
        }
    }

    private suspend fun canAffordCurrentBalance(amountCents: Long): Boolean {
        val summary = GlobalFinanceEngine.computeSummary(
            totalDepositsCents = transferDao.getDepositTotalOnce(),
            totalGoalAllocatedCents = transferDao.getGoalAllocatedTotalOnce(),
            totalExpensesCents = expenseDao.getConfirmedTotalOnce(),
        )
        return GlobalFinanceEngine.canAfford(summary, amountCents)
    }

    private suspend fun canSpendFromGoal(goalId: Long, amountCents: Long): Boolean {
        if (savingsGoalDao.getGoalById(goalId) == null) return false
        val netCents = GlobalFinanceEngine.goalNetCents(
            transferCents = transferDao.getGoalTransferTotalOnce(goalId),
            expenseCents = expenseDao.getGoalExpenseTotalOnce(goalId),
        )
        return amountCents <= netCents
    }
}
