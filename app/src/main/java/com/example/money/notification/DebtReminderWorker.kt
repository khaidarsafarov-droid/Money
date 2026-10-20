package com.example.money.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.money.MoneyApplication

class DebtReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val debtorId = inputData.getLong(KEY_DEBTOR_ID, -1L)
        if (debtorId <= 0L) return Result.failure()
        val app = applicationContext as MoneyApplication
        val snooze = inputData.getBoolean(KEY_SNOOZE, false)
        DebtReminderPresenter.showDue(app, debtorId, snooze)
        return Result.success()
    }

    companion object {
        const val KEY_DEBTOR_ID = "debtor_id"
        const val KEY_DEBTOR_NAME = "debtor_name"
        const val KEY_AMOUNT = "amount"
        const val KEY_SNOOZE = "snooze"

        fun uniqueWorkName(debtorId: Long): String = "debt_reminder_$debtorId"
    }
}
