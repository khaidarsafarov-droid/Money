package com.example.money.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.money.MoneyApplication
import com.example.money.autopilot.MicroPaymentScheduler
import com.example.money.voice.VoiceReportScheduler
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action !in RESCHEDULE_ACTIONS) return
        val app = context.applicationContext as MoneyApplication
        val pendingResult = goAsync()
        app.applicationScope.launch {
            try {
                app.rescheduleDebtReminders()
                app.syncPersonalDebtMessageSchedule()
                MicroPaymentScheduler.syncSchedule(context)
                WeeklyCheckInScheduler.syncSchedule(context)
                VoiceReportScheduler.syncSchedule(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
