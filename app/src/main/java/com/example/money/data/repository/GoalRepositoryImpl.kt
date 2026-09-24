package com.example.money.data.repository

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
import com.example.money.domain.util.MoneyMath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class GoalRepositoryImpl(
    private val savingsGoalDao: SavingsGoalDao,
    private val transferDao: TransferDao,
    private val goalActionDao: GoalActionDao,
    private val syncQueueRecorder: SyncQueueRecorder,
) : GoalRepository {

    override fun observeAllGoals(): Flow<List<SavingsGoal>> =
        combine(
            savingsGoalDao.observeAllGoals(),
            transferDao.observeGoalTransferTotals(),
            goalActionDao.observeAll(),
        ) { goals, totals, actions ->
            val allocatedByGoal = totals.associate { row ->
                row.goalId to MoneyMath.toCents(row.totalAmount)
            }
            val actionsByGoal = actions.groupBy { it.goalId }
            goals.map { entity ->
                val steps = actionsByGoal[entity.id].orEmpty()
                entity.toDomain().copy(
                    currentAmountCents = allocatedByGoal[entity.id] ?: 0L,
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
        currentAmountCents: Long,
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
                currentAmountCents = 0L,
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
            val existing = savingsGoalDao.getGoalById(goal.id) ?: return@withWriteLock
            val marked = existing.copy(updatedAt = System.currentTimeMillis())
            savingsGoalDao.delete(existing)
            syncQueueRecorder.recordGoal(marked, deleted = true)
        }
    }

    override suspend fun addAction(goalId: Long, title: String): Long =
        SyncWriteGuard.withWriteLock {
            val trimmed = title.trim()
            if (trimmed.isEmpty()) return@withWriteLock 0L
            val sortOrder = goalActionDao.maxSortOrder(goalId) + 1
            goalActionDao.insert(
                GoalActionEntity(
                    goalId = goalId,
                    title = trimmed,
                    completed = false,
                    sortOrder = sortOrder,
                ),
            )
        }

    override suspend fun setActionCompleted(actionId: Long, completed: Boolean) {
        SyncWriteGuard.withWriteLock {
            val existing = goalActionDao.getById(actionId) ?: return@withWriteLock
            goalActionDao.update(existing.copy(completed = completed))
        }
    }

    override suspend fun deleteAction(actionId: Long) {
        SyncWriteGuard.withWriteLock {
            goalActionDao.deleteById(actionId)
        }
    }
}
