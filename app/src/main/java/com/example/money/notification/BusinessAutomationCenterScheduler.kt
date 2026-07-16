package com.example.money.notification

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.money.settings.BusinessBranchSettings
import java.util.concurrent.TimeUnit

interface BusinessAutomationCenterScheduler {
    fun syncSchedule(settings: BusinessBranchSettings)
    fun cancel()
}

class BusinessAutomationCenterSchedulerImpl(
    private val context: Context,
) : BusinessAutomationCenterScheduler {

    private val workManager = WorkManager.getInstance(context)

    override fun syncSchedule(settings: BusinessBranchSettings) {
        if (!settings.hasAnyAutomationCenterJob()) {
            cancel()
            return
        }
        try {
            val request = PeriodicWorkRequestBuilder<BusinessAutomationCenterWorker>(
                24,
                TimeUnit.HOURS,
            ).build()
            workManager.enqueueUniquePeriodicWork(
                BusinessAutomationCenterWorker.uniqueWorkName(),
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        } catch (exception: Exception) {
            Log.w(TAG, "Failed to schedule business automation center", exception)
        }
    }

    override fun cancel() {
        workManager.cancelUniqueWork(BusinessAutomationCenterWorker.uniqueWorkName())
    }

    private companion object {
        const val TAG = "BusinessAutomationCenterScheduler"
    }
}
