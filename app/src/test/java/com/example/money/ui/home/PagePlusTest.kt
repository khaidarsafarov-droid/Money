package com.example.money.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PagePlusTest {

    @Test
    fun eachPageHasItsOwnPlusAction() {
        assertEquals(PagePlusAction.AddAnything, plusActionFor(HomeTab.Home))
        assertEquals(PagePlusAction.AddOperation, plusActionFor(HomeTab.BrotherFinances))
        assertEquals(PagePlusAction.AddDebt, plusActionFor(HomeTab.Debts))
        assertEquals(PagePlusAction.AddGoal, plusActionFor(HomeTab.Goals))

        assertEquals(PagePlusAction.AddAnything, plusActionFor(HomeSection.Home))
        assertEquals(PagePlusAction.AddOperation, plusActionFor(HomeSection.Operations))
        assertEquals(PagePlusAction.AddDebt, plusActionFor(HomeSection.Debts))
        assertEquals(PagePlusAction.AddGoal, plusActionFor(HomeSection.Goals))
    }

    @Test
    fun debtAndGoalPlusOpenAForm_operationsPlusRecordsMoneyOnly() {
        assertNull(plusSheetActions(PagePlusAction.AddDebt))
        assertNull(plusSheetActions(PagePlusAction.AddGoal))
        assertEquals(
            listOf(AddAction.Expense, AddAction.Deposit),
            plusSheetActions(PagePlusAction.AddOperation),
        )
        assertEquals(AddAction.entries.toList(), plusSheetActions(PagePlusAction.AddAnything))
    }
}
