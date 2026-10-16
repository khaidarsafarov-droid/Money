package com.example.money.data.repository

import androidx.room.withTransaction
import com.example.money.data.local.AppDatabase
import com.example.money.data.local.dao.ExpenseDao
import com.example.money.data.local.dao.GoalActionDao
import com.example.money.data.local.dao.SavingsGoalDao
import com.example.money.data.local.dao.TransferDao
import com.example.money.data.local.entity.GoalActionEntity
import com.example.money.data.local.entity.SavingsGoalEntity
import com.example.money.data.mapper.toDomain
import com.example.money.data.sync.SyncQueueRecorder
import com.example.money.data.sync.SyncWriteGuard
import com.example.money.domain.model.GoalAction
import com.example.money.domain.model.GoalKind
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.repository.GoalRepository
import com.example.money.domain.util.GlobalFinanceEngine
import com.example.money.domain.util.MoneyMath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class GoalRepositoryImpl(
    private val database: AppDatabase,
    private val savingsGoalDao: SavingsGoalDao,
    private val transferDao: TransferDao,
    private val expenseDao: ExpenseDao,
    private val goalActionDao: GoalActionDao,
    private val syncQueueRecorder: SyncQueueRecorder,
) : GoalRepository {

    override fun observeAllGoals(): Flow<List<SavingsGoal>> =
        combine(
            savingsGoalDao.observeAllGoals(),
            transferDao.observeGoalTransferTotals(),
            expenseDao.observeGoalExpenseTotals(),
            goalActionDao.observeAll(),
        ) { goals, totals, spent, actions ->
            val allocatedByGoal = totals.associate { row ->
                row.goalId to MoneyMath.toCents(row.totalAmount)
            }
            val spentByGoal = spent.associate { row ->
                row.goalId to MoneyMath.toCents(row.totalAmount)
            }
            val actionsByGoal = actions.groupBy { it.goalId }
            goals.map { entity ->
                val steps = actionsByGoal[entity.id].orEmpty()
                entity.toDomain().copy(
                    currentAmountCents = GlobalFinanceEngine.goalNetCents(
                        transferCents = allocatedByGoal[entity.id] ?: 0L,
                        expenseCents = spentByGoal[entity.id] ?: 0L,
                    ),
                    actionDone = steps.count { it.completed },
                    actionTotal = steps.size,
                )
            }
        }

    override fun observeActions(): Flow<List<GoalAction>> =
        goalActionDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun addGoal(
        name: String,
        targetAmountCents: Long,
        deadlineMillis: Long?,
        kind: GoalKind,
    ): Long = SyncWriteGuard.withWriteLock {
        val now = System.currentTimeMillis()
        val id = savingsGoalDao.insert(
            SavingsGoalEntity(
                name = name.trim(),
                targetAmountCents = targetAmountCents,
                currentAmountCents = 0L,
                deadlineMillis = deadlineMillis,
                updatedAt = now,
                kind = kind.name,
            ),
        )
        savingsGoalDao.getGoalById(id)?.let { syncQueueRecorder.recordGoal(it) }
        id
    }

    override suspend fun updateGoal(goal: SavingsGoal) {
        SyncWriteGuard.withWriteLock {
            val now = System.currentTimeMillis()
            val existing = savingsGoalDao.getGoalById(goal.id) ?: return@withWriteLock
            val entity = existing.copy(
                name = goal.name.trim(),
                targetAmountCents = goal.targetAmountCents,
                currentAmountCents = existing.currentAmountCents,
                currency = goal.currency,
                deadlineMillis = goal.deadlineMillis,
                updatedAt = now,
                kind = goal.kind.name,
            )
            savingsGoalDao.update(entity)
            syncQueueRecorder.recordGoal(entity)
        }
    }

    override suspend fun deleteGoal(goal: SavingsGoal) {
        SyncWriteGuard.withWriteLock {
            database.withTransaction {
                val existing = savingsGoalDao.getGoalById(goal.id) ?: return@withTransaction
                val now = System.currentTimeMillis()
                val transfers = transferDao.getByGoalId(existing.id)
                val expenses = expenseDao.getByGoalId(existing.id)
                val actions = goalActionDao.getByGoalId(existing.id)
                transfers.forEach { transferDao.delete(it) }
                expenses.forEach { expenseDao.delete(it) }
                actions.forEach { goalActionDao.deleteById(it.id) }
                val marked = existing.copy(updatedAt = now)
                savingsGoalDao.delete(existing)
                transfers.forEach { syncQueueRecorder.recordTransfer(it, deleted = true) }
                expenses.forEach { syncQueueRecorder.recordExpense(it, deleted = true) }
                actions.forEach { action ->
                    syncQueueRecorder.recordGoalAction(action, existing.syncId, deleted = true)
                }
                syncQueueRecorder.recordGoal(marked, deleted = true)
            }
        }
    }

    override suspend fun addAction(goalId: Long, title: String, completed: Boolean): Long =
        SyncWriteGuard.withWriteLock {
            val trimmed = title.trim()
            if (trimmed.isEmpty()) return@withWriteLock 0L
            val goalSyncId = savingsGoalDao.getGoalById(goalId)?.syncId ?: return@withWriteLock 0L
            val sortOrder = goalActionDao.maxSortOrder(goalId) + 1
            val id = goalActionDao.insert(
                GoalActionEntity(
                    goalId = goalId,
                    title = trimmed,
                    completed = completed,
                    sortOrder = sortOrder,
                ),
            )
            goalActionDao.getById(id)?.let { syncQueueRecorder.recordGoalAction(it, goalSyncId) }
            id
        }

    override suspend fun setActionCompleted(actionId: Long, completed: Boolean) {
        SyncWriteGuard.withWriteLock {
            val existing = goalActionDao.getById(actionId) ?: return@withWriteLock
            val goalSyncId = savingsGoalDao.getGoalById(existing.goalId)?.syncId ?: return@withWriteLock
            val updated = existing.copy(completed = completed, updatedAt = System.currentTimeMillis())
            goalActionDao.update(updated)
            syncQueueRecorder.recordGoalAction(updated, goalSyncId)
        }
    }

    override suspend fun deleteAction(actionId: Long) {
        SyncWriteGuard.withWriteLock {
            val existing = goalActionDao.getById(actionId) ?: return@withWriteLock
            val goalSyncId = savingsGoalDao.getGoalById(existing.goalId)?.syncId
            goalActionDao.deleteById(actionId)
            if (goalSyncId != null) {
                syncQueueRecorder.recordGoalAction(existing, goalSyncId, deleted = true)
            }
        }
    }
}
