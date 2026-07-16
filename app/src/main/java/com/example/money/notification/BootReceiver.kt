package com.example.money.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.money.MoneyApplication
import com.example.money.voice.VoiceReportScheduler
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as MoneyApplication
        val pendingResult = goAsync()
        app.applicationScope.launch {
            try {
                app.rescheduleDebtReminders()
                app.rescheduleBusinessReminders()
                app.syncBusinessSmsSchedule()
                app.syncWorkflowDebtSchedule()
                app.syncBusinessAutomationCenterSchedule()
                app.syncPersonalDebtMessageSchedule()
                VoiceReportScheduler.syncSchedule(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
