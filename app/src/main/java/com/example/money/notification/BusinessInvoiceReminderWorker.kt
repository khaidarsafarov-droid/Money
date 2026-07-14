package com.example.money.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.money.MoneyApplication
import com.example.money.domain.model.business.InvoiceStatus
import com.example.money.settings.BusinessBranchSettings
import kotlinx.coroutines.flow.first

class BusinessInvoiceReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as MoneyApplication
        val settings = app.preferencesStore.businessBranchSettings.first()
        if (!settings.invoiceRemindersEnabled) return Result.success()

        val invoiceId = inputData.getLong(KEY_INVOICE_ID, -1L)
        if (invoiceId == -1L) return Result.failure()

        val invoice = app.businessRepository.observeInvoices().first()
            .find { it.id == invoiceId }
            ?: return Result.success()

        if (invoice.status == InvoiceStatus.PAID) {
            BusinessNotificationHelper.cancelInvoice(applicationContext, invoiceId)
            return Result.success()
        }

        val now = System.currentTimeMillis()
        val dueAtReminder = BusinessInvoiceReminderScheduler.dueDateAtReminderHour(
            invoice.dueDate,
            settings.invoiceReminderHour,
        )

        if (now < dueAtReminder) {
            app.businessInvoiceReminderScheduler.schedule(invoice, settings)
            return Result.success()
        }

        if (BusinessNotificationHelper.isOverdue(invoice, now)) {
            val clientName = app.businessRepository.observeClients().first()
                .find { it.id == invoice.clientId }?.name
                ?: applicationContext.getString(com.example.money.R.string.business_branch_default_name)
            BusinessNotificationHelper.showOverdueInvoice(applicationContext, invoice, clientName)
            app.businessInvoiceReminderScheduler.scheduleDailyFollowUp(invoice, settings)
        }

        checkLowCash(app, settings)

        return Result.success()
    }

    private suspend fun checkLowCash(app: MoneyApplication, settings: BusinessBranchSettings) {
        if (!settings.lowCashAlertsEnabled) return
        val dashboard = app.businessRepository.observeDashboard(
            monthStartMillis = 0L,
            monthEndMillis = System.currentTimeMillis(),
            filters = com.example.money.domain.model.business.BusinessDashboardFilters(),
        ).first()
        if (dashboard.cashOnHandCents < settings.lowCashThresholdCents) {
            BusinessNotificationHelper.showLowCashAlert(
                applicationContext,
                dashboard.cashOnHandCents,
                settings.lowCashThresholdCents,
            )
        }
    }

    companion object {
        const val KEY_INVOICE_ID = "business_invoice_id"

        fun uniqueWorkName(invoiceId: Long): String = "business_invoice_reminder_$invoiceId"
    }
}
