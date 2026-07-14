package com.example.money.notification

import android.content.Context
import com.example.money.R
import com.example.money.domain.model.business.BusinessInvoice
import com.example.money.domain.model.business.InvoiceStatus
import com.example.money.ui.util.Formatters
import java.util.concurrent.TimeUnit

object BusinessNotificationHelper {

    fun showOverdueInvoice(
        context: Context,
        invoice: BusinessInvoice,
        clientName: String,
    ): Boolean {
        if (!com.example.money.ui.util.NotificationPermissions.areEnabled(context)) return false

        val now = System.currentTimeMillis()
        val daysOverdue = TimeUnit.MILLISECONDS.toDays(now - invoice.dueDate).coerceAtLeast(1)
        val amount = Formatters.currencyFromCents(invoice.amountCents)
        val body = context.getString(
            R.string.notification_business_overdue_body,
            clientName,
            amount,
            daysOverdue,
        )

        val notification = androidx.core.app.NotificationCompat.Builder(context, NotificationChannels.BUSINESS_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_business_overdue_title))
            .setContentText(body)
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        return try {
            androidx.core.app.NotificationManagerCompat.from(context)
                .notify(notificationId(invoice.id), notification)
            true
        } catch (exception: SecurityException) {
            false
        }
    }

    fun showLowCashAlert(context: Context, balanceCents: Long, thresholdCents: Long): Boolean {
        if (!com.example.money.ui.util.NotificationPermissions.areEnabled(context)) return false

        val body = context.getString(
            R.string.notification_business_low_cash_body,
            Formatters.currencyFromCents(balanceCents),
            Formatters.currencyFromCents(thresholdCents),
        )

        val notification = androidx.core.app.NotificationCompat.Builder(context, NotificationChannels.BUSINESS_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_business_low_cash_title))
            .setContentText(body)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        return try {
            androidx.core.app.NotificationManagerCompat.from(context)
                .notify(LOW_CASH_NOTIFICATION_ID, notification)
            true
        } catch (exception: SecurityException) {
            false
        }
    }

    fun cancelInvoice(context: Context, invoiceId: Long) {
        androidx.core.app.NotificationManagerCompat.from(context)
            .cancel(notificationId(invoiceId))
    }

    fun cancelAll(context: Context) {
        androidx.core.app.NotificationManagerCompat.from(context)
            .cancel(LOW_CASH_NOTIFICATION_ID)
    }

    fun isOverdue(invoice: BusinessInvoice, now: Long = System.currentTimeMillis()): Boolean =
        invoice.status != InvoiceStatus.PAID && invoice.dueDate < now

    private const val LOW_CASH_NOTIFICATION_ID = 90_001

    private fun notificationId(invoiceId: Long): Int = (100_000 + invoiceId).toInt()
}
