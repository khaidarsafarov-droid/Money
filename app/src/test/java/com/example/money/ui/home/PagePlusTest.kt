package com.example.money.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PagePlusTest {

    @Test
    fun eachPageHasItsOwnPlusAction() {
        assertEquals(PagePlusAction.AddAnything, plusActionFor(HomeTab.Home))
        assertEquals(PagePlusAction.AddNote, plusActionFor(HomeTab.Notes))
        assertEquals(PagePlusAction.AddDebt, plusActionFor(HomeTab.Debts))
        assertEquals(PagePlusAction.AddGoal, plusActionFor(HomeTab.Goals))
        assertEquals(PagePlusAction.AddAnything, plusActionFor(HomeTab.More))

        assertEquals(PagePlusAction.AddAnything, plusActionFor(HomeSection.Home))
        assertEquals(PagePlusAction.AddNote, plusActionFor(HomeSection.Notes))
        assertEquals(PagePlusAction.AddDebt, plusActionFor(HomeSection.Debts))
        assertEquals(PagePlusAction.AddGoal, plusActionFor(HomeSection.Goals))
    }

    @Test
    fun homePlusOffersGoalsAndDebts_notesPlusOpensAForm() {
        assertNull(plusSheetActions(PagePlusAction.AddDebt))
        assertNull(plusSheetActions(PagePlusAction.AddGoal))
        assertNull(plusSheetActions(PagePlusAction.AddNote))
        assertEquals(
            listOf(AddAction.NewGoal, AddAction.Debt),
            plusSheetActions(PagePlusAction.AddAnything),
        )
    }

    @Test
    fun plusStaysOffHomeAndMore() {
        assertFalse(showsPageFab(HomeTab.Home, tablet = false))
        assertFalse(showsPageFab(HomeTab.More, tablet = false))
        assertTrue(showsPageFab(HomeTab.Debts, tablet = false))
        assertTrue(showsPageFab(HomeTab.Goals, tablet = false))
        assertTrue(showsPageFab(HomeTab.Notes, tablet = false))
        assertTrue(showsPageFab(HomeTab.More, tablet = true))
    }
}
