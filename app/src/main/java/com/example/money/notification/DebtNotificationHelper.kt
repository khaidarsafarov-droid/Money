package com.example.money.notification

import android.content.Context
import com.example.money.R
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.ui.util.Formatters
import java.util.concurrent.TimeUnit

object DebtNotificationHelper {

    private const val REQUEST_CODE_CONTENT = 0
    private const val REQUEST_CODE_MARK_PAID = 1
    private const val REQUEST_CODE_SNOOZE = 2

    fun show(context: Context, debtor: Debtor): Boolean {
        if (!com.example.money.ui.util.NotificationPermissions.areEnabled(context)) return false

        val body = formatBody(context, debtor)
        val notification = androidx.core.app.NotificationCompat.Builder(context, NotificationChannels.DEBT_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_debt_due_title))
            .setContentText(body)
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
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
            .build()

        return try {
            androidx.core.app.NotificationManagerCompat.from(context)
                .notify(debtor.id.toInt(), notification)
            true
        } catch (exception: SecurityException) {
            false
        }
    }

    fun cancel(context: Context, debtorId: Long) {
        androidx.core.app.NotificationManagerCompat.from(context)
            .cancel(debtorId.toInt())
    }

    fun formatBody(context: Context, debtor: Debtor): String {
        val amount = Formatters.currency(debtor.amount)
        val now = System.currentTimeMillis()
        val isOverdue = !debtor.isPaid && debtor.dateDue < now
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

    private fun createContentIntent(context: Context, debtorId: Long): android.app.PendingIntent =
        android.app.PendingIntent.getActivity(
            context,
            notificationRequestCode(debtorId, REQUEST_CODE_CONTENT),
            com.example.money.navigation.AppIntents.openDebtorIntent(context, debtorId),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

    private fun createMarkPaidIntent(context: Context, debtorId: Long): android.app.PendingIntent {
        val intent = android.content.Intent(context, DebtNotificationActionReceiver::class.java).apply {
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
        val intent = android.content.Intent(context, DebtNotificationActionReceiver::class.java).apply {
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

    private fun notificationRequestCode(debtorId: Long, actionCode: Int): Int =
        (debtorId * 10 + actionCode).toInt()
}
