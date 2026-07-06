package com.example.money.domain.repository

import com.example.money.domain.model.BudgetSummary
import com.example.money.domain.model.Expense
import com.example.money.domain.model.Transfer
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {

    fun getTransfers(): Flow<List<Transfer>>

    fun getExpenses(): Flow<List<Expense>>

    fun observeBudgetSummary(): Flow<BudgetSummary>

    fun observePendingCount(): Flow<Int>

    suspend fun addDeposit(transfer: Transfer): Long

    suspend fun addGoalTransfer(transfer: Transfer): Long

    suspend fun addTransfer(transfer: Transfer): Long

    suspend fun addExpense(expense: Expense): Long

    suspend fun confirmTransfer(id: Long)

    suspend fun confirmExpense(id: Long)

    suspend fun deleteTransfer(transfer: Transfer)

    suspend fun deleteExpense(expense: Expense)
}
