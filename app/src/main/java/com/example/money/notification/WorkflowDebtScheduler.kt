package com.example.money.notification

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.money.settings.BusinessBranchSettings
import java.util.concurrent.TimeUnit

interface WorkflowDebtScheduler {
    fun syncSchedule(settings: BusinessBranchSettings)
    fun cancel()
}

class WorkflowDebtSchedulerImpl(
    private val context: Context,
) : WorkflowDebtScheduler {

    private val workManager = WorkManager.getInstance(context)

    override fun syncSchedule(settings: BusinessBranchSettings) {
        if (!settings.hasWorkflowDebtAutomation() || settings.hasAnyAutomationCenterJob()) {
            cancel()
            return
        }
        try {
            val request = PeriodicWorkRequestBuilder<WorkflowDebtReminderWorker>(
                24,
                TimeUnit.HOURS,
            ).build()
            workManager.enqueueUniquePeriodicWork(
                WorkflowDebtReminderWorker.uniqueWorkName(),
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        } catch (exception: Exception) {
            Log.w(TAG, "Failed to schedule workflow debt reminders", exception)
        }
    }

    override fun cancel() {
        workManager.cancelUniqueWork(WorkflowDebtReminderWorker.uniqueWorkName())
    }

    private companion object {
        const val TAG = "WorkflowDebtScheduler"
    }
}
