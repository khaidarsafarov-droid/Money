package com.example.money.notification

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.money.settings.PersonalBranchSettings
import java.util.concurrent.TimeUnit

interface PersonalDebtMessageScheduler {
    fun syncSchedule(settings: PersonalBranchSettings)
    fun cancel()
}

class PersonalDebtMessageSchedulerImpl(
    private val context: Context,
) : PersonalDebtMessageScheduler {

    private val workManager = WorkManager.getInstance(context)

    override fun syncSchedule(settings: PersonalBranchSettings) {
        if (!settings.debtAutoMessageEnabled) {
            cancel()
            return
        }
        try {
            val request = PeriodicWorkRequestBuilder<PersonalDebtMessageWorker>(
                24,
                TimeUnit.HOURS,
            ).build()
            workManager.enqueueUniquePeriodicWork(
                PersonalDebtMessageWorker.uniqueWorkName(),
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        } catch (exception: Exception) {
            Log.w(TAG, "Failed to schedule personal debt auto-messages", exception)
        }
    }

    override fun cancel() {
        workManager.cancelUniqueWork(PersonalDebtMessageWorker.uniqueWorkName())
    }

    private companion object {
        const val TAG = "PersonalDebtMessageScheduler"
    }
}
