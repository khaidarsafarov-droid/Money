package com.example.money.domain.repository

import com.example.money.domain.model.GoalAction
import com.example.money.domain.model.GoalKind
import com.example.money.domain.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {

    fun observeAllGoals(): Flow<List<SavingsGoal>>

    fun observeActions(): Flow<List<GoalAction>>

    suspend fun addGoal(
        name: String,
        targetAmountCents: Long,
        deadlineMillis: Long? = null,
        kind: GoalKind = GoalKind.MONEY,
    ): Long

    suspend fun updateGoal(goal: SavingsGoal)

    suspend fun deleteGoal(goal: SavingsGoal)

    suspend fun addAction(goalId: Long, title: String, completed: Boolean = false): Long

    suspend fun setActionCompleted(actionId: Long, completed: Boolean)

    suspend fun deleteAction(actionId: Long)
}
