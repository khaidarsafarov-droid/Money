package com.example.money.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.money.domain.model.BudgetSummary
import com.example.money.domain.model.Expense
import com.example.money.domain.model.Transfer
import com.example.money.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class BudgetActionResult {
    data object Success : BudgetActionResult()
    data object InsufficientFunds : BudgetActionResult()
}

class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
) : ViewModel() {

    val transfers: StateFlow<List<Transfer>> = budgetRepository.getTransfers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val expenses: StateFlow<List<Expense>> = budgetRepository.getExpenses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val budgetSummary: StateFlow<BudgetSummary> = budgetRepository.observeBudgetSummary()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BudgetSummary(0.0, 0.0, 0.0),
        )

    fun addDeposit(amount: Double, date: Long, onResult: (BudgetActionResult) -> Unit = {}) {
        viewModelScope.launch {
            budgetRepository.addDeposit(Transfer(amount = amount, date = date))
            onResult(BudgetActionResult.Success)
        }
    }

    fun addGoalTransfer(
        goalId: Long,
        amount: Double,
        date: Long,
        onResult: (BudgetActionResult) -> Unit = {},
    ) {
        viewModelScope.launch {
            val added = budgetRepository.addGoalTransferIfAffordable(
                Transfer(amount = amount, date = date, goalId = goalId),
            )
            onResult(if (added) BudgetActionResult.Success else BudgetActionResult.InsufficientFunds)
        }
    }

    fun addTransfer(amount: Double, date: Long) {
        addDeposit(amount, date)
    }

    fun addExpense(
        amount: Double,
        category: String,
        date: Long,
        note: String,
        goalId: Long? = null,
        onResult: (BudgetActionResult) -> Unit = {},
    ) {
        viewModelScope.launch {
            val added = budgetRepository.addExpenseIfAffordable(
                Expense(
                    amount = amount,
                    category = category,
                    date = date,
                    note = note,
                    goalId = goalId,
                ),
            )
            onResult(if (added) BudgetActionResult.Success else BudgetActionResult.InsufficientFunds)
        }
    }

    fun deleteTransfer(transfer: Transfer, debtorRepository: com.example.money.domain.repository.DebtorRepository? = null) {
        viewModelScope.launch {
            if (transfer.debtId != null && debtorRepository != null) {
                debtorRepository.handleTransferDeleted(transfer)
            }
            budgetRepository.deleteTransfer(transfer)
        }
    }

    fun deleteExpense(expense: Expense, debtorRepository: com.example.money.domain.repository.DebtorRepository? = null) {
        viewModelScope.launch {
            if (expense.debtId != null && debtorRepository != null) {
                debtorRepository.handleExpenseDeleted(expense)
            }
            budgetRepository.deleteExpense(expense)
        }
    }

    fun confirmTransfer(id: Long) {
        viewModelScope.launch {
            budgetRepository.confirmTransfer(id)
        }
    }

    fun confirmExpense(id: Long) {
        viewModelScope.launch {
            budgetRepository.confirmExpense(id)
        }
    }

    class Factory(
        private val budgetRepository: BudgetRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {
                return BudgetViewModel(budgetRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
