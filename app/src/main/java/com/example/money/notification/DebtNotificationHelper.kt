package com.example.money.notification

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.money.R
import com.example.money.data.locale.AppLanguageStorage
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.domain.util.DebtDueDateUtils
import com.example.money.messaging.MessagingChannel
import com.example.money.messaging.UniversalMessageSender
import com.example.money.settings.AppMode
import com.example.money.settings.CurrencyRegion
import com.example.money.ui.debtors.DebtReminderShare
import com.example.money.ui.util.Formatters
import java.util.Currency
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
    private const val REQUEST_CODE_CONFIRM_PAID = 8

    fun show(
        context: Context,
        debtor: Debtor,
        currencyRegion: CurrencyRegion = Formatters.activeRegion,
        clientName: String? = null,
        company: String? = null,
    ): Boolean {
        val localized = localizedContext(context)
        if (!com.example.money.ui.util.NotificationPermissions.areEnabled(localized)) return false

        val body = decorate(
            localized,
            debtor,
            formatBody(localized, debtor, currencyRegion),
            clientName,
            company,
        )
        val contact = DebtReminderShare.resolveContact(localized, debtor)
        val builder = NotificationCompat.Builder(localized, channelFor(debtor))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localized.getString(R.string.notification_debt_due_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(buildExpandedBody(localized, debtor, body)))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(createContentIntent(localized, debtor.id))
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_check,
                    localized.getString(R.string.notification_action_mark_paid),
                    createMarkPaidIntent(localized, debtor.id),
                ).build(),
            )
            .addAction(
                R.drawable.ic_snooze,
                localized.getString(R.string.notification_action_snooze),
                createSnoozeIntent(localized, debtor),
            )

        if (contact.phone.isNotBlank() && UniversalMessageSender.canSend(MessagingChannel.WHATSAPP, contact)) {
            builder.addAction(
                R.drawable.ic_notification,
                localized.getString(R.string.notification_action_whatsapp),
                createMessagingIntent(localized, debtor.id, MessagingChannel.WHATSAPP),
            )
        }
        if (contact.phone.isNotBlank() && UniversalMessageSender.canSend(MessagingChannel.SMS, contact)) {
            builder.addAction(
                R.drawable.ic_notification,
                localized.getString(R.string.notification_action_sms),
                createMessagingIntent(localized, debtor.id, MessagingChannel.SMS),
            )
        }
        if (contact.email.isNotBlank() && UniversalMessageSender.canSend(MessagingChannel.EMAIL, contact)) {
            builder.addAction(
                R.drawable.ic_notification,
                localized.getString(R.string.notification_action_email),
                createMessagingIntent(localized, debtor.id, MessagingChannel.EMAIL),
            )
        }

        return notify(context, debtor.id.toInt(), builder.build())
    }

    fun showAdvanceReminder(
        context: Context,
        debtor: Debtor,
        daysBefore: Int,
        currencyRegion: CurrencyRegion = Formatters.activeRegion,
        clientName: String? = null,
        company: String? = null,
    ): Boolean {
        val localized = localizedContext(context)
        if (!com.example.money.ui.util.NotificationPermissions.areEnabled(localized)) return false

        val amount = money(localized, debtor.remainingAmountCents, currencyRegion)
        val dueDate = Formatters.shortDate(debtor.dateDue)
        val title = when (daysBefore) {
            3 -> localized.getString(R.string.notification_debt_advance_3d_title)
            else -> localized.getString(R.string.notification_debt_advance_1d_title)
        }
        val body = decorate(
            localized,
            debtor,
            localized.getString(
                R.string.notification_debt_advance_body,
                debtor.name,
                amount,
                dueDate,
            ),
            clientName,
            company,
        )

        val notification = NotificationCompat.Builder(localized, NotificationChannels.DUE_SOON)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(createContentIntent(localized, debtor.id))
            .addAction(
                R.drawable.ic_notification,
                localized.getString(R.string.notification_action_view_debt),
                createContentIntent(localized, debtor.id),
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
        val localized = localizedContext(context)
        if (!com.example.money.ui.util.NotificationPermissions.areEnabled(localized)) return false

        val channelLabel = when (channel) {
            MessagingChannel.WHATSAPP -> localized.getString(R.string.messaging_channel_whatsapp)
            MessagingChannel.TELEGRAM -> localized.getString(R.string.messaging_channel_telegram)
            MessagingChannel.EMAIL -> localized.getString(R.string.messaging_channel_email)
            MessagingChannel.SMS -> localized.getString(R.string.messaging_channel_sms)
        }
        val body = localized.getString(R.string.notification_debt_auto_message_body, channelLabel, debtor.name)

        val notification = NotificationCompat.Builder(localized, NotificationChannels.DEBT_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_notification,
                localized.getString(R.string.notification_action_send_now, channelLabel),
                createMessagingIntent(localized, debtor.id, channel),
            )
            .build()

        val notificationId = (debtor.id * 100 + channel.ordinal).toInt()
        return notify(context, notificationId, notification)
    }

    fun cancel(context: Context, debtorId: Long) {
        val manager = NotificationManagerCompat.from(context)
        cancel(manager, debtorId.toInt())
        cancel(manager, (debtorId + CONFIRM_ID_OFFSET).toInt())
        MessagingChannel.entries.forEach { channel ->
            cancel(manager, (debtorId * 100 + channel.ordinal).toInt())
        }
        listOf(1, 3).forEach { days ->
            cancel(manager, (debtorId * 10 + days).toInt())
        }
    }

    fun cancelAll(context: Context) {
        try {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.activeNotifications
                .filter { notification ->
                    notification.tag == NOTIFICATION_TAG ||
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                            notification.notification.channelId in NotificationChannels.debtChannelIds)
                }
                .forEach { notification ->
                    manager.cancel(notification.tag, notification.id)
                }
        } catch (_: Exception) {
            // Notification cleanup is best-effort; WorkManager cancellation still prevents new stale alerts.
        }
    }

    fun formatBody(
        context: Context,
        debtor: Debtor,
        currencyRegion: CurrencyRegion = Formatters.activeRegion,
    ): String {
        val amount = money(context, debtor.remainingAmountCents, currencyRegion)
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

    fun showPaidConfirmation(context: Context, debtor: Debtor): Boolean {
        val localized = localizedContext(context)
        if (!com.example.money.ui.util.NotificationPermissions.areEnabled(localized)) return false
        val confirm = NotificationCompat.Action.Builder(
            R.drawable.ic_check,
            localized.getString(R.string.notification_action_confirm_paid),
            createConfirmPaidIntent(localized, debtor.id),
        ).setAuthenticationRequired(true).build()
        val notification = NotificationCompat.Builder(localized, channelFor(debtor))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localized.getString(R.string.notification_confirm_paid_title))
            .setContentText(localized.getString(R.string.notification_confirm_paid_body, debtor.name))
            .setAutoCancel(true)
            .addAction(confirm)
            .build()
        return notify(localized, (debtor.id + CONFIRM_ID_OFFSET).toInt(), notification)
    }

    internal fun localizedContext(context: Context): Context {
        val locale = AppLanguageStorage.read(context).toLocale() ?: return context
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    private fun money(context: Context, cents: Long, region: CurrencyRegion): String {
        val locales = context.resources.configuration.locales
        val locale = if (locales.isEmpty) java.util.Locale.getDefault() else locales[0]
        val format = java.text.NumberFormat.getCurrencyInstance(locale)
        format.currency = Currency.getInstance(region.currencyCode)
        return format.format(cents / 100.0)
    }

    private fun channelFor(debtor: Debtor, now: Long = System.currentTimeMillis()): String = when {
        DebtDueDateUtils.isOverdue(debtor.dateDue, now) -> NotificationChannels.OVERDUE
        DebtDueDateUtils.startOfDayMillis(debtor.dateDue) == DebtDueDateUtils.startOfDayMillis(now) ->
            NotificationChannels.DUE_TODAY
        else -> NotificationChannels.DUE_SOON
    }

    private fun decorate(
        context: Context,
        debtor: Debtor,
        body: String,
        clientName: String?,
        company: String?,
    ): String {
        val modeLabel = context.getString(
            if (debtor.mode == AppMode.PROFESSIONAL) R.string.mode_professional else R.string.mode_personal,
        )
        val lines = mutableListOf(body, context.getString(R.string.notification_mode_line, modeLabel))
        if (debtor.mode == AppMode.PROFESSIONAL && !clientName.isNullOrBlank()) {
            val companySuffix = company?.takeIf { it.isNotBlank() }?.let { ", $it" }.orEmpty()
            lines += context.getString(R.string.notification_client_line, clientName, companySuffix)
        }
        return lines.joinToString("\n")
    }

    private fun createConfirmPaidIntent(context: Context, debtorId: Long): android.app.PendingIntent {
        val intent = Intent(context, DebtNotificationActionReceiver::class.java).apply {
            action = DebtNotificationActionReceiver.ACTION_CONFIRM_MARK_PAID
            putExtra(DebtNotificationActionReceiver.EXTRA_DEBTOR_ID, debtorId)
        }
        return android.app.PendingIntent.getBroadcast(
            context,
            notificationRequestCode(debtorId, REQUEST_CODE_CONFIRM_PAID),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notify(context: Context, id: Int, notification: android.app.Notification): Boolean =
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_TAG, id, notification)
            true
        } catch (_: SecurityException) {
            false
        }

    private fun cancel(manager: NotificationManagerCompat, id: Int) {
        manager.cancel(NOTIFICATION_TAG, id)
        manager.cancel(id)
    }

    private fun notificationRequestCode(debtorId: Long, actionCode: Int): Int =
        (debtorId * 10 + actionCode).toInt()

    private const val NOTIFICATION_TAG = "debt_reminder"
    private const val CONFIRM_ID_OFFSET = 50_000L
}
