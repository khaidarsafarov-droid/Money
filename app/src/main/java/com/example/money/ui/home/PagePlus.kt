package com.example.money.ui.home

import com.example.money.R

/** What the page plus button does. The dock no longer owns this control. */
internal enum class PagePlusAction {
    AddAnything,
    AddDebt,
    AddGoal,
    AddNote,
}

internal fun plusActionFor(tab: HomeTab): PagePlusAction = when (tab) {
    HomeTab.Home -> PagePlusAction.AddAnything
    HomeTab.Notes -> PagePlusAction.AddNote
    HomeTab.Debts -> PagePlusAction.AddDebt
    HomeTab.Goals -> PagePlusAction.AddGoal
}

internal fun plusActionFor(section: HomeSection): PagePlusAction = when (section) {
    HomeSection.Home -> PagePlusAction.AddAnything
    HomeSection.Notes -> PagePlusAction.AddNote
    HomeSection.Debts -> PagePlusAction.AddDebt
    HomeSection.Goals -> PagePlusAction.AddGoal
}

/** Sheet contents for pages that offer more than one record. Null opens a form directly. */
internal fun plusSheetActions(action: PagePlusAction): List<AddAction>? = when (action) {
    PagePlusAction.AddAnything -> listOf(AddAction.NewGoal, AddAction.Debt)
    PagePlusAction.AddDebt, PagePlusAction.AddGoal, PagePlusAction.AddNote -> null
}

internal fun plusHighlight(action: PagePlusAction): AddAction = when (action) {
    PagePlusAction.AddAnything, PagePlusAction.AddGoal -> AddAction.NewGoal
    PagePlusAction.AddDebt -> AddAction.Debt
    PagePlusAction.AddNote -> AddAction.NewGoal
}

internal fun plusContentRes(action: PagePlusAction): Int = when (action) {
    PagePlusAction.AddAnything -> R.string.add_sheet_title
    PagePlusAction.AddDebt -> R.string.empty_debtors_action
    PagePlusAction.AddGoal -> R.string.empty_goals_action
    PagePlusAction.AddNote -> R.string.note_add_title
}
