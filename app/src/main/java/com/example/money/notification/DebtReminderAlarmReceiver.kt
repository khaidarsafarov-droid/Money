package com.example.money.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.money.MoneyApplication
import kotlinx.coroutines.launch

class DebtReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != DebtReminderAlarms.ACTION) return
        val debtorId = intent.getLongExtra(DebtReminderAlarms.EXTRA_DEBTOR_ID, -1L)
        if (debtorId <= 0L) return
        val app = context.applicationContext as MoneyApplication
        val pendingResult = goAsync()
        val kind = intent.getStringExtra(DebtReminderAlarms.EXTRA_KIND)
        val daysBefore = intent.getIntExtra(DebtReminderAlarms.EXTRA_DAYS_BEFORE, 1)
        val snooze = intent.getBooleanExtra(DebtReminderAlarms.EXTRA_SNOOZE, false)
        app.applicationScope.launch {
            try {
                if (kind == DebtReminderAlarms.KIND_ADVANCE) {
                    DebtReminderPresenter.showAdvance(app, debtorId, daysBefore)
                } else {
                    DebtReminderPresenter.showDue(app, debtorId, snooze)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
