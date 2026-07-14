package com.example.money.notification

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.money.domain.model.business.BusinessInvoice
import com.example.money.domain.model.business.InvoiceStatus
import com.example.money.settings.BusinessBranchSettings
import java.util.Calendar
import java.util.concurrent.TimeUnit

interface BusinessInvoiceReminderScheduler {

    fun schedule(invoice: BusinessInvoice, settings: BusinessBranchSettings)

    fun scheduleDailyFollowUp(invoice: BusinessInvoice, settings: BusinessBranchSettings)

    fun cancel(invoiceId: Long)

    fun syncAll(invoices: List<BusinessInvoice>, settings: BusinessBranchSettings)

    companion object {
        fun dueDateAtReminderHour(dueMillis: Long, hour: Int): Long {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = dueMillis
                set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return calendar.timeInMillis
        }

        fun delayUntilDueReminder(
            dueMillis: Long,
            reminderHour: Int,
            now: Long = System.currentTimeMillis(),
        ): Long {
            val dueAtReminder = dueDateAtReminderHour(dueMillis, reminderHour)
            return if (dueAtReminder > now) {
                dueAtReminder - now
            } else {
                delayUntilNextDailyReminder(reminderHour, now)
            }
        }

        fun delayUntilNextDailyReminder(
            reminderHour: Int,
            now: Long = System.currentTimeMillis(),
        ): Long {
            val calendar = Calendar.getInstance().apply { timeInMillis = now }
            calendar.set(Calendar.HOUR_OF_DAY, reminderHour.coerceIn(0, 23))
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            if (calendar.timeInMillis <= now) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis - now
        }
    }
}

class BusinessInvoiceReminderSchedulerImpl(
    private val context: Context,
) : BusinessInvoiceReminderScheduler {

    private val workManager = WorkManager.getInstance(context)

    override fun schedule(invoice: BusinessInvoice, settings: BusinessBranchSettings) {
        if (!settings.invoiceRemindersEnabled || invoice.status == InvoiceStatus.PAID || invoice.id == 0L) {
            return
        }
        try {
            val delay = BusinessInvoiceReminderScheduler.delayUntilDueReminder(
                invoice.dueDate,
                settings.invoiceReminderHour,
            )
            enqueue(invoice.id, delay)
        } catch (exception: Exception) {
            Log.w(TAG, "Failed to schedule business reminder for invoice ${invoice.id}", exception)
        }
    }

    override fun scheduleDailyFollowUp(invoice: BusinessInvoice, settings: BusinessBranchSettings) {
        if (!settings.invoiceRemindersEnabled || invoice.status == InvoiceStatus.PAID || invoice.id == 0L) {
            return
        }
        enqueue(
            invoice.id,
            BusinessInvoiceReminderScheduler.delayUntilNextDailyReminder(settings.invoiceReminderHour),
        )
    }

    override fun cancel(invoiceId: Long) {
        workManager.cancelUniqueWork(BusinessInvoiceReminderWorker.uniqueWorkName(invoiceId))
        BusinessNotificationHelper.cancelInvoice(context, invoiceId)
    }

    override fun syncAll(invoices: List<BusinessInvoice>, settings: BusinessBranchSettings) {
        if (!settings.invoiceRemindersEnabled) {
            invoices.forEach { cancel(it.id) }
            return
        }
        invoices.filter { it.status != InvoiceStatus.PAID }.forEach { schedule(it, settings) }
    }

    private fun enqueue(invoiceId: Long, delayMillis: Long) {
        val safeDelay = delayMillis.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS)
        val inputData = Data.Builder()
            .putLong(BusinessInvoiceReminderWorker.KEY_INVOICE_ID, invoiceId)
            .build()

        val request = OneTimeWorkRequestBuilder<BusinessInvoiceReminderWorker>()
            .setInitialDelay(safeDelay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(BusinessInvoiceReminderWorker.uniqueWorkName(invoiceId))
            .build()

        workManager.enqueueUniqueWork(
            BusinessInvoiceReminderWorker.uniqueWorkName(invoiceId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private companion object {
        const val TAG = "BusinessInvoiceReminderScheduler"
        val MIN_DELAY_MS = TimeUnit.SECONDS.toMillis(1)
        val MAX_DELAY_MS = TimeUnit.DAYS.toMillis(365)
    }
}
