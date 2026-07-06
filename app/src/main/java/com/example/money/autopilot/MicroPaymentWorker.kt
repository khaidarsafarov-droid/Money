package com.example.money.autopilot

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.money.MoneyApplication
import com.example.money.R
import com.example.money.domain.util.DebtAutopilotEngine
import com.example.money.navigation.AppIntents
import com.example.money.notification.NotificationChannels
import com.example.money.ui.util.Formatters
import com.example.money.ui.util.NotificationPermissions
import kotlinx.coroutines.flow.first

class MicroPaymentWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as MoneyApplication
        val settings = app.preferencesStore.autopilotSettings.first()
        val debts = app.debtorRepository.getAllDebtors().first()
        val plan = DebtAutopilotEngine.planMicroPayment(debts, settings)

        if (plan != null) {
            showReminder(applicationContext, plan.amountCents, plan.debtorId)
        }

        MicroPaymentScheduler.enqueueNextFromSettings(applicationContext)
        return Result.success()
    }

    private fun showReminder(context: Context, amountCents: Long, debtorId: Long) {
        if (!NotificationPermissions.areEnabled(context)) return

        val intent = AppIntents.openDebtPaymentIntent(context, debtorId)
        val pendingIntent = PendingIntent.getActivity(
            context,
            debtorId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val body = context.getString(
            R.string.autopilot_notification_body,
            Formatters.currencyFromCents(amountCents),
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.DEBT_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.autopilot_notification_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    private companion object {
        const val NOTIFICATION_ID = 77_001
    }
}
