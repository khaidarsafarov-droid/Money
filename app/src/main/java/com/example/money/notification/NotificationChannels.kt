package com.example.money.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.money.R

object NotificationChannels {

    const val DEBT_REMINDERS = "debt_reminders"
    const val BUSINESS_ALERTS = "business_alerts"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val debtChannel = NotificationChannel(
            DEBT_REMINDERS,
            context.getString(R.string.notification_channel_debt_title),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_debt_description)
        }
        manager.createNotificationChannel(debtChannel)

        val businessChannel = NotificationChannel(
            BUSINESS_ALERTS,
            context.getString(R.string.notification_channel_business_title),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_business_description)
        }
        manager.createNotificationChannel(businessChannel)
    }
}
