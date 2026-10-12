package com.example.money.ui.home

import com.example.money.R
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.SavingsGoal

sealed class HomeListSelection {
    data object Overview : HomeListSelection()
    data class GoalItem(val goal: SavingsGoal) : HomeListSelection()
    data class DebtorItem(val debtor: Debtor) : HomeListSelection()
}

enum class HomeSection(val titleRes: Int) {
    Home(R.string.nav_home),
    Goals(R.string.section_goals),
    Debts(R.string.tab_debts),
    Notes(R.string.tab_notes),
}
