package com.example.money.ui.home

import com.example.money.R

/** What the page plus button does. The dock no longer owns this control. */
internal enum class PagePlusAction {
    AddAnything,
    AddOperation,
    AddDebt,
    AddGoal,
}

internal fun plusActionFor(tab: HomeTab): PagePlusAction = when (tab) {
    HomeTab.Home -> PagePlusAction.AddAnything
    HomeTab.BrotherFinances -> PagePlusAction.AddOperation
    HomeTab.Debts -> PagePlusAction.AddDebt
    HomeTab.Goals -> PagePlusAction.AddGoal
}

internal fun plusActionFor(section: HomeSection): PagePlusAction = when (section) {
    HomeSection.Home -> PagePlusAction.AddAnything
    HomeSection.Operations -> PagePlusAction.AddOperation
    HomeSection.Debts -> PagePlusAction.AddDebt
    HomeSection.Goals -> PagePlusAction.AddGoal
}

/** Sheet contents for pages that offer more than one record. Null opens a form directly. */
internal fun plusSheetActions(action: PagePlusAction): List<AddAction>? = when (action) {
    PagePlusAction.AddAnything -> AddAction.entries
    PagePlusAction.AddOperation -> listOf(AddAction.Expense, AddAction.Deposit)
    PagePlusAction.AddDebt, PagePlusAction.AddGoal -> null
}

internal fun plusHighlight(action: PagePlusAction): AddAction = when (action) {
    PagePlusAction.AddAnything, PagePlusAction.AddOperation -> AddAction.Expense
    PagePlusAction.AddDebt -> AddAction.Debt
    PagePlusAction.AddGoal -> AddAction.NewGoal
}

internal fun plusContentRes(action: PagePlusAction): Int = when (action) {
    PagePlusAction.AddAnything -> R.string.add_sheet_title
    PagePlusAction.AddOperation -> R.string.empty_budget_action
    PagePlusAction.AddDebt -> R.string.empty_debtors_action
    PagePlusAction.AddGoal -> R.string.empty_goals_action
}
