package com.example.money.notification

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.money.R
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.domain.util.DebtDueDateUtils
import com.example.money.messaging.MessagingChannel
import com.example.money.messaging.UniversalMessageSender
import com.example.money.ui.debtors.DebtReminderShare
import com.example.money.ui.util.Formatters
import java.util.concurrent.TimeUnit

object DebtNotificationHelper {

    private const val REQUEST_CODE_CONTENT = 0
    private const val REQUEST_CODE_MARK_PAID = 1
    private const val REQUEST_CODE_SNOOZE = 2
    private const val REQUEST_CODE_WHATSAPP = 3
    private const val REQUEST_CODE_SMS = 4
    private const val REQUEST_CODE_EMAIL = 5
    private const val REQUEST_CODE_TELEGRAM = 6
    private const val REQUEST_CODE_MESSAGING = 7

    fun show(context: Context, debtor: Debtor): Boolean {
        if (!com.example.money.ui.util.NotificationPermissions.areEnabled(context)) return false

        val body = formatBody(context, debtor)
        val contact = DebtReminderShare.resolveContact(context, debtor)
        val builder = NotificationCompat.Builder(context, NotificationChannels.DEBT_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_debt_due_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(buildExpandedBody(context, debtor, body)))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(createContentIntent(context, debtor.id))
            .addAction(
                R.drawable.ic_check,
                context.getString(R.string.notification_action_mark_paid),
                createMarkPaidIntent(context, debtor.id),
            )
            .addAction(
                R.drawable.ic_snooze,
                context.getString(R.string.notification_action_snooze),
                createSnoozeIntent(context, debtor),
            )

        if (contact.phone.isNotBlank() && UniversalMessageSender.canSend(MessagingChannel.WHATSAPP, contact)) {
            builder.addAction(
                R.drawable.ic_notification,
                context.getString(R.string.notification_action_whatsapp),
                createMessagingIntent(context, debtor.id, MessagingChannel.WHATSAPP),
            )
        }
        if (contact.phone.isNotBlank() && UniversalMessageSender.canSend(MessagingChannel.SMS, contact)) {
            builder.addAction(
                R.drawable.ic_notification,
                context.getString(R.string.notification_action_sms),
                createMessagingIntent(context, debtor.id, MessagingChannel.SMS),
            )
        }
        if (contact.email.isNotBlank() && UniversalMessageSender.canSend(MessagingChannel.EMAIL, contact)) {
            builder.addAction(
                R.drawable.ic_notification,
                context.getString(R.string.notification_action_email),
                createMessagingIntent(context, debtor.id, MessagingChannel.EMAIL),
            )
        }

        return notify(context, debtor.id.toInt(), builder.build())
    }

    fun showAdvanceReminder(context: Context, debtor: Debtor, daysBefore: Int): Boolean {
        if (!com.example.money.ui.util.NotificationPermissions.areEnabled(context)) return false

        val amount = Formatters.currency(debtor.amount)
        val dueDate = Formatters.shortDate(debtor.dateDue)
        val title = when (daysBefore) {
            3 -> context.getString(R.string.notification_debt_advance_3d_title)
            else -> context.getString(R.string.notification_debt_advance_1d_title)
        }
        val body = context.getString(
            R.string.notification_debt_advance_body,
            debtor.name,
            amount,
            dueDate,
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.DEBT_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(createContentIntent(context, debtor.id))
            .addAction(
                R.drawable.ic_notification,
                context.getString(R.string.notification_action_view_debt),
                createContentIntent(context, debtor.id),
            )
            .build()

        return notify(context, (debtor.id * 10 + daysBefore).toInt(), notification)
    }

    fun showMessagingAction(
        context: Context,
        debtor: Debtor,
        channel: MessagingChannel,
        title: String,
    ): Boolean {
        if (!com.example.money.ui.util.NotificationPermissions.areEnabled(context)) return false

        val channelLabel = when (channel) {
            MessagingChannel.WHATSAPP -> context.getString(R.string.messaging_channel_whatsapp)
            MessagingChannel.TELEGRAM -> context.getString(R.string.messaging_channel_telegram)
            MessagingChannel.EMAIL -> context.getString(R.string.messaging_channel_email)
            MessagingChannel.SMS -> context.getString(R.string.messaging_channel_sms)
        }
        val body = context.getString(R.string.notification_debt_auto_message_body, channelLabel, debtor.name)

        val notification = NotificationCompat.Builder(context, NotificationChannels.DEBT_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_notification,
                context.getString(R.string.notification_action_send_now, channelLabel),
                createMessagingIntent(context, debtor.id, channel),
            )
            .build()

        val notificationId = (debtor.id * 100 + channel.ordinal).toInt()
        return notify(context, notificationId, notification)
    }

    fun cancel(context: Context, debtorId: Long) {
        NotificationManagerCompat.from(context).cancel(debtorId.toInt())
        MessagingChannel.entries.forEach { channel ->
            NotificationManagerCompat.from(context).cancel((debtorId * 100 + channel.ordinal).toInt())
        }
        listOf(1, 3).forEach { days ->
            NotificationManagerCompat.from(context).cancel((debtorId * 10 + days).toInt())
        }
    }

    fun formatBody(context: Context, debtor: Debtor): String {
        val amount = Formatters.currency(debtor.amount)
        val now = System.currentTimeMillis()
        val isOverdue = !debtor.isPaid && DebtDueDateUtils.isOverdue(debtor.dateDue, now)
        val daysOverdue = if (isOverdue) {
            TimeUnit.MILLISECONDS.toDays(now - debtor.dateDue).coerceAtLeast(1)
        } else {
            0L
        }

        return when {
            isOverdue && debtor.debtType == DebtType.DEBTOR -> context.getString(
                R.string.notification_debt_overdue_i_owe,
                amount,
                debtor.name,
                daysOverdue,
            )
            isOverdue -> context.getString(
                R.string.notification_debt_overdue_owes_me,
                debtor.name,
                amount,
                daysOverdue,
            )
            debtor.debtType == DebtType.DEBTOR -> context.getString(
                R.string.notification_debt_due_i_owe,
                amount,
                debtor.name,
            )
            else -> context.getString(
                R.string.notification_debt_due_body,
                debtor.name,
                amount,
            )
        }
    }

    private fun buildExpandedBody(context: Context, debtor: Debtor, baseBody: String): String {
        val contact = DebtReminderShare.resolveContact(context, debtor)
        val dueDate = Formatters.shortDate(debtor.dateDue)
        val lines = buildList {
            add(baseBody)
            add(context.getString(R.string.notification_debt_detail_due, dueDate))
            if (contact.phone.isNotBlank()) {
                add(context.getString(R.string.notification_debt_detail_phone, contact.phone))
            }
            if (contact.email.isNotBlank()) {
                add(context.getString(R.string.notification_debt_detail_email, contact.email))
            }
        }
        return lines.joinToString("\n")
    }

    private fun createContentIntent(context: Context, debtorId: Long): android.app.PendingIntent =
        android.app.PendingIntent.getActivity(
            context,
            notificationRequestCode(debtorId, REQUEST_CODE_CONTENT),
            com.example.money.navigation.AppIntents.openDebtorIntent(context, debtorId),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

    private fun createMarkPaidIntent(context: Context, debtorId: Long): android.app.PendingIntent {
        val intent = Intent(context, DebtNotificationActionReceiver::class.java).apply {
            action = DebtNotificationActionReceiver.ACTION_MARK_PAID
            putExtra(DebtNotificationActionReceiver.EXTRA_DEBTOR_ID, debtorId)
        }
        return android.app.PendingIntent.getBroadcast(
            context,
            notificationRequestCode(debtorId, REQUEST_CODE_MARK_PAID),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createSnoozeIntent(context: Context, debtor: Debtor): android.app.PendingIntent {
        val intent = Intent(context, DebtNotificationActionReceiver::class.java).apply {
            action = DebtNotificationActionReceiver.ACTION_SNOOZE
            putExtra(DebtNotificationActionReceiver.EXTRA_DEBTOR_ID, debtor.id)
            putExtra(DebtNotificationActionReceiver.EXTRA_DEBTOR_NAME, debtor.name)
            putExtra(DebtNotificationActionReceiver.EXTRA_DEBTOR_AMOUNT, debtor.amount)
        }
        return android.app.PendingIntent.getBroadcast(
            context,
            notificationRequestCode(debtor.id, REQUEST_CODE_SNOOZE),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createMessagingIntent(
        context: Context,
        debtorId: Long,
        channel: MessagingChannel,
    ): android.app.PendingIntent {
        val intent = Intent(context, DebtNotificationActionReceiver::class.java).apply {
            action = DebtNotificationActionReceiver.ACTION_SEND_MESSAGE
            putExtra(DebtNotificationActionReceiver.EXTRA_DEBTOR_ID, debtorId)
            putExtra(DebtNotificationActionReceiver.EXTRA_MESSAGING_CHANNEL, channel.name)
        }
        val requestCode = when (channel) {
            MessagingChannel.WHATSAPP -> REQUEST_CODE_WHATSAPP
            MessagingChannel.SMS -> REQUEST_CODE_SMS
            MessagingChannel.EMAIL -> REQUEST_CODE_EMAIL
            MessagingChannel.TELEGRAM -> REQUEST_CODE_TELEGRAM
        }
        return android.app.PendingIntent.getBroadcast(
            context,
            notificationRequestCode(debtorId, requestCode),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notify(context: Context, id: Int, notification: android.app.Notification): Boolean =
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
            true
        } catch (_: SecurityException) {
            false
        }

    private fun notificationRequestCode(debtorId: Long, actionCode: Int): Int =
        (debtorId * 10 + actionCode).toInt()
}
