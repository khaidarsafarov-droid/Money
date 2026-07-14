package com.example.money.notification

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.money.settings.BusinessBranchSettings
import java.util.concurrent.TimeUnit

interface BusinessSmsScheduler {
    fun syncSchedule(settings: BusinessBranchSettings)
    fun cancel()
}

class BusinessSmsSchedulerImpl(
    private val context: Context,
) : BusinessSmsScheduler {

    private val workManager = WorkManager.getInstance(context)

    override fun syncSchedule(settings: BusinessBranchSettings) {
        if (!settings.smsAutomationEnabled) {
            cancel()
            return
        }
        try {
            val request = PeriodicWorkRequestBuilder<BusinessSmsReminderWorker>(
                24,
                TimeUnit.HOURS,
            ).build()
            workManager.enqueueUniquePeriodicWork(
                BusinessSmsReminderWorker.uniqueWorkName(),
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        } catch (exception: Exception) {
            Log.w(TAG, "Failed to schedule business SMS reminders", exception)
        }
    }

    override fun cancel() {
        workManager.cancelUniqueWork(BusinessSmsReminderWorker.uniqueWorkName())
    }

    private companion object {
        const val TAG = "BusinessSmsScheduler"
    }
}
