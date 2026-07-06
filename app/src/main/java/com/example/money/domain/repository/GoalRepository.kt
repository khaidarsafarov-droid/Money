package com.example.money.domain.repository

import com.example.money.domain.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {

    fun observeAllGoals(): Flow<List<SavingsGoal>>

    suspend fun addGoal(
        name: String,
        targetAmountCents: Long,
        currentAmountCents: Long,
        deadlineMillis: Long? = null,
    ): Long

    suspend fun updateGoal(goal: SavingsGoal)

    suspend fun deleteGoal(goal: SavingsGoal)
}
