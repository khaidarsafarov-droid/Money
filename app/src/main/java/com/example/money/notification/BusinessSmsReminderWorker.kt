package com.example.money.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.money.MoneyApplication
import kotlinx.coroutines.flow.first

class BusinessSmsReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as MoneyApplication
        val settings = app.preferencesStore.businessBranchSettings.first()
        if (!settings.smsAutomationEnabled || !settings.smsOverdueInvoiceEnabled) {
            return Result.success()
        }

        app.businessSmsUseCases.buildOverdueInvoiceSms(
            businessName = settings.businessName.ifBlank { "Business" },
            useDirectSend = settings.smsDirectSendEnabled,
        )
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "business_sms_overdue_reminder"

        fun uniqueWorkName(): String = UNIQUE_WORK_NAME
    }
}
