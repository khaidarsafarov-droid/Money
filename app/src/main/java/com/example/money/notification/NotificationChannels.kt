package com.example.money.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.money.R

object NotificationChannels {

    const val DEBT_REMINDERS = "debt_reminders"
    const val DUE_TODAY = "debt_due_today"
    const val DUE_SOON = "debt_due_soon"
    const val OVERDUE = "debt_overdue"
    const val WEEKLY_REVIEW = "weekly_review"

    val debtChannelIds: Set<String> = setOf(DEBT_REMINDERS, DUE_TODAY, DUE_SOON, OVERDUE, WEEKLY_REVIEW)

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        listOf(
            channel(context, DEBT_REMINDERS, R.string.notification_channel_debt_title, R.string.notification_channel_debt_description, NotificationManager.IMPORTANCE_HIGH),
            channel(context, DUE_TODAY, R.string.notification_channel_due_today, R.string.notification_channel_due_today_description, NotificationManager.IMPORTANCE_HIGH),
            channel(context, DUE_SOON, R.string.notification_channel_due_soon, R.string.notification_channel_due_soon_description, NotificationManager.IMPORTANCE_DEFAULT),
            channel(context, OVERDUE, R.string.notification_channel_overdue, R.string.notification_channel_overdue_description, NotificationManager.IMPORTANCE_HIGH),
            channel(context, WEEKLY_REVIEW, R.string.notification_channel_weekly, R.string.notification_channel_weekly_description, NotificationManager.IMPORTANCE_LOW),
        ).forEach(manager::createNotificationChannel)
    }

    private fun channel(
        context: Context,
        id: String,
        titleRes: Int,
        descriptionRes: Int,
        importance: Int,
    ): NotificationChannel = NotificationChannel(
        id,
        context.getString(titleRes),
        importance,
    ).apply {
        description = context.getString(descriptionRes)
    }
}
