package com.example.money.data.repository

import com.example.money.data.local.dao.ExpenseDao
import com.example.money.data.local.dao.SavingsGoalDao
import com.example.money.data.local.dao.TransferDao
import com.example.money.data.mapper.toDomain
import com.example.money.data.mapper.toEntity
import com.example.money.data.sync.SyncQueueRecorder
import com.example.money.domain.model.BudgetSummary
import com.example.money.domain.model.Expense
import com.example.money.domain.model.Transfer
import com.example.money.domain.repository.BudgetRepository
import com.example.money.domain.util.GlobalFinanceEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl(
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
                totalDeposits = totalDeposits,
                totalGoalAllocated = totalGoalAllocated,
                totalExpenses = totalExpenses,
            )
            BudgetSummary(
                totalDeposits = summary.totalDeposits,
                totalGoalAllocated = summary.totalGoalAllocated,
                totalExpenses = summary.totalExpenses,
            )
        }

    override fun observePendingCount(): Flow<Int> =
        combine(
            transferDao.observePendingCount(),
            expenseDao.observePendingCount(),
        ) { transfers, expenses -> transfers + expenses }

    override suspend fun addDeposit(transfer: Transfer): Long {
        val id = transferDao.insert(
            transfer.copy(isConfirmed = true, goalId = null).toEntity(),
        )
        transferDao.getById(id)?.let { syncQueueRecorder.recordTransfer(it) }
        return id
    }

    override suspend fun addGoalTransfer(transfer: Transfer): Long {
        val goalSyncId = transfer.goalId?.let { savingsGoalDao.getGoalById(it)?.syncId }
        val id = transferDao.insert(
            transfer.copy(isConfirmed = true).toEntity().copy(goalSyncId = goalSyncId),
        )
        transferDao.getById(id)?.let { syncQueueRecorder.recordTransfer(it) }
        return id
    }

    override suspend fun addTransfer(transfer: Transfer): Long = addDeposit(transfer)

    override suspend fun addExpense(expense: Expense): Long {
        val id = expenseDao.insert(expense.copy(isConfirmed = true).toEntity())
        expenseDao.getById(id)?.let { syncQueueRecorder.recordExpense(it) }
        return id
    }

    override suspend fun confirmTransfer(id: Long) {
        transferDao.confirm(id)
        transferDao.getById(id)?.let { syncQueueRecorder.recordTransfer(it) }
    }

    override suspend fun confirmExpense(id: Long) {
        expenseDao.confirm(id)
        expenseDao.getById(id)?.let { syncQueueRecorder.recordExpense(it) }
    }

    override suspend fun deleteTransfer(transfer: Transfer) {
        val existing = transferDao.getById(transfer.id) ?: return
        transferDao.delete(existing)
        syncQueueRecorder.recordTransfer(existing, deleted = true)
    }

    override suspend fun deleteExpense(expense: Expense) {
        val existing = expenseDao.getById(expense.id) ?: return
        expenseDao.delete(existing)
        syncQueueRecorder.recordExpense(existing, deleted = true)
    }
}
