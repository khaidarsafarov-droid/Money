package com.example.money.ui.home

import com.example.money.R
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.Expense
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.model.Transfer

sealed class HomeListSelection {
    data object Overview : HomeListSelection()
    data class GoalItem(val goal: SavingsGoal) : HomeListSelection()
    data class DebtorItem(val debtor: Debtor) : HomeListSelection()
    data class TransferItem(val transfer: Transfer, val goalName: String?) : HomeListSelection()
    data class ExpenseItem(val expense: Expense) : HomeListSelection()
}

enum class HomeSection(val titleRes: Int) {
    Goals(R.string.section_goals),
    Debts(R.string.tab_debts),
    Operations(R.string.tab_operations),
}
