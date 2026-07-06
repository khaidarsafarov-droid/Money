package com.example.money.navigation

import android.content.Context
import android.content.Intent
import com.example.money.MainActivity
import com.example.money.ui.home.HomeLaunchConfig
import com.example.money.ui.home.HomeTab

object AppIntents {

    const val EXTRA_ACTION = "extra_action"
    const val EXTRA_DEBTOR_ID = "extra_debtor_id"

    const val ACTION_ADD_EXPENSE = "add_expense"
    const val ACTION_QUICK_ADD = "quick_add"
    const val ACTION_OPEN_DEBTOR = "open_debtor"
    const val ACTION_DEBT_PAYMENT = "debt_payment"

    fun parseLaunchConfig(intent: Intent?): HomeLaunchConfig {
        if (intent == null) return HomeLaunchConfig()

        return when (intent.getStringExtra(EXTRA_ACTION)) {
            ACTION_ADD_EXPENSE -> HomeLaunchConfig(openExpenseDialog = true)
            ACTION_QUICK_ADD -> HomeLaunchConfig(openQuickAddDialog = true)
            ACTION_OPEN_DEBTOR -> {
                val debtorId = intent.getLongExtra(EXTRA_DEBTOR_ID, -1L)
                HomeLaunchConfig(
                    initialTab = HomeTab.Debts,
                    highlightDebtorId = debtorId.takeIf { it > 0L },
                )
            }
            ACTION_DEBT_PAYMENT -> {
                val debtorId = intent.getLongExtra(EXTRA_DEBTOR_ID, -1L)
                HomeLaunchConfig(
                    initialTab = HomeTab.Debts,
                    openDebtPaymentForDebtorId = debtorId.takeIf { it > 0L },
                )
            }
            else -> HomeLaunchConfig()
        }
    }

    fun addExpenseIntent(context: Context): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION, ACTION_ADD_EXPENSE)
        }

    fun openAppIntent(context: Context): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

    fun quickAddIntent(context: Context): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION, ACTION_QUICK_ADD)
        }

    fun openDebtorIntent(context: Context, debtorId: Long): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION, ACTION_OPEN_DEBTOR)
            putExtra(EXTRA_DEBTOR_ID, debtorId)
        }

    fun openDebtPaymentIntent(context: Context, debtorId: Long): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION, ACTION_DEBT_PAYMENT)
            putExtra(EXTRA_DEBTOR_ID, debtorId)
        }

    /**
     * Opens the React web dashboard payment modal via ?pay= deep link.
     * Requires [baseUrl] to be the deployed web app origin (no trailing slash).
     */
    fun openWebPaymentDeepLink(context: Context, baseUrl: String, payId: String): Intent {
        val normalized = baseUrl.trim().trimEnd('/')
        val uri = android.net.Uri.parse(normalized).buildUpon()
            .appendQueryParameter("pay", payId)
            .build()
        return Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
