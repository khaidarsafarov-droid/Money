package com.example.money.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.money.MoneyApplication
import com.example.money.domain.model.DebtType
import kotlinx.coroutines.launch

class DebtNotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val app = context.applicationContext as MoneyApplication
        val pendingResult = goAsync()

        app.applicationScope.launch {
            try {
                when (intent.action) {
                    ACTION_MARK_PAID -> handleMarkPaid(app, context, intent)
                    ACTION_SNOOZE -> handleSnooze(app, context, intent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleMarkPaid(
        app: MoneyApplication,
        context: Context,
        intent: Intent,
    ) {
        val debtorId = intent.getLongExtra(EXTRA_DEBTOR_ID, -1L)
        if (debtorId <= 0L) return

        val debtor = app.debtorRepository.getDebtorById(debtorId) ?: return
        val affectsBalance = debtor.debtType == DebtType.CREDITOR
        app.debtorRepository.markAsPaid(debtorId, affectsBalance)
        app.debtReminderScheduler.cancel(debtorId)
        DebtNotificationHelper.cancel(context, debtorId)
    }

    private suspend fun handleSnooze(
        app: MoneyApplication,
        context: Context,
        intent: Intent,
    ) {
        val debtorId = intent.getLongExtra(EXTRA_DEBTOR_ID, -1L)
        if (debtorId <= 0L) return

        val debtor = app.debtorRepository.getDebtorById(debtorId) ?: return
        app.debtReminderScheduler.snooze(debtor)
        DebtNotificationHelper.cancel(context, debtorId)
    }

    companion object {
        const val ACTION_MARK_PAID = "com.example.money.action.MARK_DEBT_PAID"
        const val ACTION_SNOOZE = "com.example.money.action.SNOOZE_DEBT_REMINDER"

        const val EXTRA_DEBTOR_ID = "extra_debtor_id"
        const val EXTRA_DEBTOR_NAME = "extra_debtor_name"
        const val EXTRA_DEBTOR_AMOUNT = "extra_debtor_amount"
    }
}
