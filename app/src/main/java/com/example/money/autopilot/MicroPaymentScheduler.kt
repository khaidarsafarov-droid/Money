package com.example.money.autopilot

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.money.MoneyApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

object MicroPaymentScheduler {

    private const val UNIQUE_WORK_NAME = "micro_payment_daily"

    suspend fun syncSchedule(context: Context) {
        val app = context.applicationContext as MoneyApplication
        val settings = app.preferencesStore.autopilotSettings.first()
        if (settings.enabled) {
            enqueue(context, settings.reminderHour, settings.reminderMinute)
        } else {
            cancel(context)
        }
    }

    fun enqueueNextFromSettings(context: Context) {
        val app = context.applicationContext as? MoneyApplication ?: return
        app.applicationScope.launch {
            syncSchedule(context)
        }
    }

    fun enqueue(context: Context, hour: Int, minute: Int) {
        val delayMs = computeDelayMillis(hour, minute)
        val request = OneTimeWorkRequestBuilder<MicroPaymentWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag(UNIQUE_WORK_NAME)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    internal fun computeDelayMillis(
        hour: Int,
        minute: Int,
        now: Calendar = Calendar.getInstance(),
    ): Long {
        val target = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return (target.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }
}
