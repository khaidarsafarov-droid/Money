package com.example.money.ui.home

import com.example.money.domain.model.BudgetSummary
import com.example.money.domain.model.DashboardDebtMetrics
import com.example.money.domain.model.DashboardGoalMetrics
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.Expense
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.model.Transfer
import com.example.money.settings.AutopilotSettings
import com.example.money.settings.GamificationState
import com.example.money.settings.PrivacySettings
import com.example.money.ui.debtors.DebtTab
import com.example.money.ui.debtors.DebtorFilter

/**
 * Snapshot of home dashboard state — reduces scattered collectAsState calls
 * and limits recomposition to a single state object when needed.
 */
data class HomeUiState(
    val budgetSummary: BudgetSummary,
    val goalMetrics: DashboardGoalMetrics,
    val debtMetrics: DashboardDebtMetrics,
    val goals: List<SavingsGoal>,
    val debtors: List<Debtor>,
    val allDebtors: List<Debtor>,
    val transfers: List<Transfer>,
    val expenses: List<Expense>,
    val debtTab: DebtTab,
    val debtorFilter: DebtorFilter,
    val debtSearchQuery: String,
    val debtProgressPercent: Int,
    val privacySettings: PrivacySettings,
    val gamificationState: GamificationState,
    val autopilotSettings: AutopilotSettings,
)
