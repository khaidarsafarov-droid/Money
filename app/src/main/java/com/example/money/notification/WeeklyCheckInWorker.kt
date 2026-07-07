package com.example.money.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.money.R
import com.example.money.navigation.AppIntents
import com.example.money.notification.NotificationChannels
import com.example.money.ui.util.NotificationPermissions

class WeeklyCheckInWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        showCheckInReminder(applicationContext)
        WeeklyCheckInScheduler.enqueueNextFromSettings(applicationContext)
        return Result.success()
    }

    private fun showCheckInReminder(context: Context) {
        if (!NotificationPermissions.areEnabled(context)) return

        val intent = AppIntents.openWeeklyCheckInIntent(context)
        val pendingIntent = PendingIntent.getActivity(
            context,
            CHECKIN_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val body = context.getString(R.string.weekly_checkin_notification_body)

        val notification = NotificationCompat.Builder(context, NotificationChannels.DEBT_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.weekly_checkin_notification_title))
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
        const val NOTIFICATION_ID = 77_002
        const val CHECKIN_REQUEST_CODE = 77002
    }
}
