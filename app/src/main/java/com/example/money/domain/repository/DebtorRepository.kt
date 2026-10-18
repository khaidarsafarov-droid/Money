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

    /** Gives more money to an existing debt. Increases that debt instead of creating another one. */
    suspend fun addDebtAdvance(debtId: Long, amountCents: Long, givenAt: Long, note: String): Boolean

    suspend fun updateDebtor(debtor: Debtor, previousAffectsBalance: Boolean)

    suspend fun processDebtPayment(debtId: Long, paymentCents: Long, affectsBalance: Boolean): Boolean

    /** Same write as [processDebtPayment]. Returns the stored payment id so the caller can undo it. */
    suspend fun recordDebtPayment(debtId: Long, paymentCents: Long, affectsBalance: Boolean): Long?

    suspend fun undoDebtPayment(debtId: Long, paymentId: Long)

    suspend fun getDebtorById(id: Long): Debtor?

    suspend fun markAsPaid(id: Long, affectsBalance: Boolean = false)

    suspend fun deleteDebtor(debtor: Debtor)

    suspend fun handleTransferDeleted(transfer: Transfer)

    suspend fun handleExpenseDeleted(expense: Expense)
}
