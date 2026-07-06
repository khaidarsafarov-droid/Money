package com.example.money.navigation

import android.content.Intent
import com.example.money.ui.home.HomeTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppIntentsTest {

    @Test
    fun parseLaunchConfig_addExpense() {
        val intent = Intent().apply {
            putExtra(AppIntents.EXTRA_ACTION, AppIntents.ACTION_ADD_EXPENSE)
        }
        val config = AppIntents.parseLaunchConfig(intent)
        assertTrue(config.openExpenseDialog)
    }

    @Test
    fun parseLaunchConfig_openDebtor() {
        val intent = Intent().apply {
            putExtra(AppIntents.EXTRA_ACTION, AppIntents.ACTION_OPEN_DEBTOR)
            putExtra(AppIntents.EXTRA_DEBTOR_ID, 42L)
        }
        val config = AppIntents.parseLaunchConfig(intent)
        assertEquals(HomeTab.Debts, config.initialTab)
        assertEquals(42L, config.highlightDebtorId)
    }

    @Test
    fun parseLaunchConfig_quickAdd() {
        val intent = Intent().apply {
            putExtra(AppIntents.EXTRA_ACTION, AppIntents.ACTION_QUICK_ADD)
        }
        val config = AppIntents.parseLaunchConfig(intent)
        assertTrue(config.openQuickAddDialog)
    }

    @Test
    fun parseLaunchConfig_emptyIntent() {
        val config = AppIntents.parseLaunchConfig(Intent())
        assertNull(config.highlightDebtorId)
    }
}
