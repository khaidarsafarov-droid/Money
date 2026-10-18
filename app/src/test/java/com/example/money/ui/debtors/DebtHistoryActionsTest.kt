package com.example.money.ui.debtors

import org.junit.Assert.assertEquals
import org.junit.Test

class DebtHistoryActionsTest {

    @Test
    fun tookButtonUsesSolidErrorInLightTheme() {
        assertEquals(HistoryTookFill.Error, historyTookFill(darkTheme = false))
    }

    @Test
    fun tookButtonUsesErrorContainerInDarkTheme() {
        assertEquals(HistoryTookFill.ErrorContainer, historyTookFill(darkTheme = true))
    }
}
