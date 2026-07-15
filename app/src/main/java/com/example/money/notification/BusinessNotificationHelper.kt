package com.example.money.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.money.R
import com.example.money.domain.model.business.BusinessInvoice
import com.example.money.domain.model.business.InvoiceStatus
import com.example.money.messaging.MessagePayload
import com.example.money.messaging.MessagingChannel
import com.example.money.messaging.MessagingContact
import com.example.money.messaging.UniversalMessageSender
import com.example.money.domain.model.business.workflow.WorkflowDebtWithParticipant
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

    fun showOverdueMessagingAction(
        context: Context,
        invoice: BusinessInvoice,
        clientName: String,
        channel: MessagingChannel,
        contact: MessagingContact,
        body: String,
    ): Boolean {
        if (!com.example.money.ui.util.NotificationPermissions.areEnabled(context)) return false

        val channelLabel = when (channel) {
            MessagingChannel.WHATSAPP -> context.getString(R.string.messaging_channel_whatsapp)
            MessagingChannel.TELEGRAM -> context.getString(R.string.messaging_channel_telegram)
            MessagingChannel.EMAIL -> context.getString(R.string.messaging_channel_email)
            MessagingChannel.SMS -> context.getString(R.string.messaging_channel_sms)
        }
        val title = context.getString(R.string.notification_business_overdue_title)
        val notificationBody = context.getString(
            R.string.notification_debt_auto_message_body,
            channelLabel,
            clientName,
        )
        val launchIntent = UniversalMessageSender.createLaunchIntent(
            context,
            channel,
            contact,
            MessagePayload(body = body),
        ) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val pendingIntent = PendingIntent.getActivity(
            context,
            messagingRequestCode(invoice.id, channel),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = androidx.core.app.NotificationCompat.Builder(context, NotificationChannels.BUSINESS_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(notificationBody)
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(notificationBody))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_notification,
                context.getString(R.string.notification_action_send_now, channelLabel),
                pendingIntent,
            )
            .build()

        return try {
            androidx.core.app.NotificationManagerCompat.from(context)
                .notify(messagingNotificationId(invoice.id, channel), notification)
            true
        } catch (exception: SecurityException) {
            false
        }
    }

    fun showWorkflowDebtReminder(
        context: Context,
        debt: WorkflowDebtWithParticipant,
        billTitle: String,
        body: String,
    ): Boolean {
        if (!com.example.money.ui.util.NotificationPermissions.areEnabled(context)) return false

        val amount = Formatters.currencyFromCents(debt.debt.remainingAmountCents)
        val title = context.getString(R.string.notification_workflow_debt_title, debt.participant.name)
        val notificationBody = if (billTitle.isNotBlank()) {
            context.getString(
                R.string.notification_workflow_debt_body,
                billTitle,
                amount,
                body,
            )
        } else {
            context.getString(R.string.notification_workflow_debt_body_short, amount, body)
        }

        val builder = androidx.core.app.NotificationCompat.Builder(context, NotificationChannels.BUSINESS_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(notificationBody)
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(notificationBody))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val phone = debt.participant.phone.trim()
        if (phone.isNotBlank()) {
            val contact = MessagingContact(phone = phone, name = debt.participant.name)
            val launchIntent = UniversalMessageSender.createLaunchIntent(
                context,
                MessagingChannel.WHATSAPP,
                contact,
                MessagePayload(body = body),
            )
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    workflowDebtRequestCode(debt.debt.id),
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                builder.setContentIntent(pendingIntent)
                builder.addAction(
                    R.drawable.ic_notification,
                    context.getString(
                        R.string.notification_action_send_now,
                        context.getString(R.string.messaging_channel_whatsapp),
                    ),
                    pendingIntent,
                )
            }
        }

        return try {
            androidx.core.app.NotificationManagerCompat.from(context)
                .notify(workflowDebtNotificationId(debt.debt.id), builder.build())
            true
        } catch (exception: SecurityException) {
            false
        }
    }

    fun cancelWorkflowDebt(context: Context, debtId: Long) {
        androidx.core.app.NotificationManagerCompat.from(context)
            .cancel(workflowDebtNotificationId(debtId))
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
        MessagingChannel.entries.forEach { channel ->
            androidx.core.app.NotificationManagerCompat.from(context)
                .cancel(messagingNotificationId(invoiceId, channel))
        }
    }

    fun cancelAll(context: Context) {
        androidx.core.app.NotificationManagerCompat.from(context)
            .cancel(LOW_CASH_NOTIFICATION_ID)
    }

    fun isOverdue(invoice: BusinessInvoice, now: Long = System.currentTimeMillis()): Boolean =
        invoice.status != InvoiceStatus.PAID && invoice.dueDate < now

    private const val LOW_CASH_NOTIFICATION_ID = 90_001

    private fun notificationId(invoiceId: Long): Int = (100_000 + invoiceId).toInt()

    private fun messagingNotificationId(invoiceId: Long, channel: MessagingChannel): Int =
        (200_000 + invoiceId * 10 + channel.ordinal).toInt()

    private fun messagingRequestCode(invoiceId: Long, channel: MessagingChannel): Int =
        (300_000 + invoiceId * 10 + channel.ordinal).toInt()

    private fun workflowDebtNotificationId(debtId: Long): Int = (400_000 + debtId).toInt()

    private fun workflowDebtRequestCode(debtId: Long): Int = (500_000 + debtId).toInt()
}
