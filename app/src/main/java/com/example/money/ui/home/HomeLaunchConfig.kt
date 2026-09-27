package com.example.money.ui.home

import com.example.money.R
import com.example.money.ui.debtors.DebtTab

data class HomeLaunchConfig(
    val openExpenseDialog: Boolean = false,
    val openQuickAddDialog: Boolean = false,
    val initialTab: HomeTab = HomeTab.Home,
    val initialDebtTab: DebtTab = DebtTab.OwesMe,
    val highlightDebtorId: Long? = null,
    val openDebtPaymentForDebtorId: Long? = null,
    val openWeeklyCheckIn: Boolean = false,
) {
    fun withoutOneShotActions(): HomeLaunchConfig = copy(
        openExpenseDialog = false,
        openQuickAddDialog = false,
        highlightDebtorId = null,
        openDebtPaymentForDebtorId = null,
        openWeeklyCheckIn = false,
    )
}

enum class HomeTab(val titleRes: Int) {
    Home(R.string.nav_home),
    Goals(R.string.section_goals),
    Debts(R.string.tab_debts),
    BrotherFinances(R.string.tab_operations),
}
