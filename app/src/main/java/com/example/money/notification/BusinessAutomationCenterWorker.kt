package com.example.money.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.money.MoneyApplication
import kotlinx.coroutines.flow.first

class BusinessAutomationCenterWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as MoneyApplication
        val settings = app.preferencesStore.businessBranchSettings.first()
        if (!settings.hasAnyAutomationCenterJob()) {
            return Result.success()
        }

        app.businessAutomationCenterUseCases.runAll(applicationContext, settings)
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "business_automation_center"

        fun uniqueWorkName(): String = UNIQUE_WORK_NAME
    }
}
