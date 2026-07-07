package com.example.money.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.money.MoneyApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WeeklyCheckInScheduler {

    private const val UNIQUE_WORK_NAME = "weekly_debt_checkin"

    suspend fun syncSchedule(context: Context) {
        val app = context.applicationContext as MoneyApplication
        val settings = app.preferencesStore.weeklyCheckInSettings.first()
        if (settings.enabled) {
            enqueue(context, settings.hour)
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

    fun enqueue(context: Context, hour: Int) {
        val delayMs = computeDelayMillis(hour)
        val request = OneTimeWorkRequestBuilder<WeeklyCheckInWorker>()
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
        now: Calendar = Calendar.getInstance(),
    ): Long {
        val target = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            while (timeInMillis <= now.timeInMillis || get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
        return (target.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }
}
