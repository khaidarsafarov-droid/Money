package com.example.money.domain.repository

import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.Expense
import com.example.money.domain.model.Transfer
import com.example.money.ui.debtors.DebtTab
import com.example.money.ui.debtors.DebtorFilter
import kotlinx.coroutines.flow.Flow

interface DebtorRepository {

    fun getAllDebtors(): Flow<List<Debtor>>

    fun getActiveDebtors(): Flow<List<Debtor>>

    /** SQL-backed list for tab/filter/search — avoids loading full table into memory. */
    fun observeFilteredDebtors(
        tab: DebtTab,
        filter: DebtorFilter,
        searchQuery: String,
        now: Long = System.currentTimeMillis(),
    ): Flow<List<Debtor>>

    suspend fun addDebtor(debtor: Debtor): Long

    suspend fun updateDebtor(debtor: Debtor, previousAffectsBalance: Boolean)

    suspend fun processDebtPayment(debtId: Long, paymentCents: Long, affectsBalance: Boolean): Boolean

    suspend fun getDebtorById(id: Long): Debtor?

    suspend fun markAsPaid(id: Long, affectsBalance: Boolean = false)

    suspend fun deleteDebtor(debtor: Debtor)

    suspend fun handleTransferDeleted(transfer: Transfer)

    suspend fun handleExpenseDeleted(expense: Expense)
}
