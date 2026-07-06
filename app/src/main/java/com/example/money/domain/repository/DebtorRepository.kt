package com.example.money.domain.repository

import com.example.money.domain.model.Debtor
import com.example.money.domain.model.Expense
import com.example.money.domain.model.Transfer
import kotlinx.coroutines.flow.Flow

interface DebtorRepository {

    fun getAllDebtors(): Flow<List<Debtor>>

    fun getActiveDebtors(): Flow<List<Debtor>>

    suspend fun addDebtor(debtor: Debtor): Long

    suspend fun updateDebtor(debtor: Debtor, previousAffectsBalance: Boolean)

    suspend fun processDebtPayment(debtId: Long, paymentCents: Long, affectsBalance: Boolean): Boolean

    suspend fun getDebtorById(id: Long): Debtor?

    suspend fun markAsPaid(id: Long, affectsBalance: Boolean = true)

    suspend fun deleteDebtor(debtor: Debtor)

    suspend fun handleTransferDeleted(transfer: Transfer)

    suspend fun handleExpenseDeleted(expense: Expense)
}
