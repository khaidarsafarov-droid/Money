package com.example.money.navigation

import android.content.Intent
import android.net.Uri
import com.example.money.ui.debtors.DebtTab
import com.example.money.ui.home.HomeTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppIntentsTest {

    @Test
    fun parseLaunchConfig_opensDebtFromMoneyDeepLink() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("money://debt/42"))

        val config = AppIntents.parseLaunchConfig(intent)

        assertEquals(HomeTab.Debts, config.initialTab)
        assertEquals(42L, config.highlightDebtorId)
    }

    @Test
    fun parseLaunchConfig_ignoresInvalidDebtDeepLink() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("money://debt/not-a-number"))

        val config = AppIntents.parseLaunchConfig(intent)

        assertNull(config.highlightDebtorId)
    }

    @Test
    fun parseLaunchConfig_opensOwesMeDebts() {
        val config = AppIntents.parseLaunchConfig(
            Intent().putExtra(AppIntents.EXTRA_ACTION, AppIntents.ACTION_OPEN_OWES_ME),
        )

        assertEquals(HomeTab.Debts, config.initialTab)
        assertEquals(DebtTab.OwesMe, config.initialDebtTab)
    }

    @Test
    fun parseLaunchConfig_opensIOweDebts() {
        val config = AppIntents.parseLaunchConfig(
            Intent().putExtra(AppIntents.EXTRA_ACTION, AppIntents.ACTION_OPEN_I_OWE),
        )

        assertEquals(HomeTab.Debts, config.initialTab)
        assertEquals(DebtTab.IOwe, config.initialDebtTab)
    }

    @Test
    fun parseLaunchConfig_opensGoalsTab() {
        val config = AppIntents.parseLaunchConfig(
            Intent().putExtra(AppIntents.EXTRA_ACTION, AppIntents.ACTION_OPEN_GOALS),
        )

        assertEquals(HomeTab.Goals, config.initialTab)
    }
}
