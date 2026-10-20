package com.example.money.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.money.MoneyApplication

class DebtAdvanceReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val debtorId = inputData.getLong(KEY_DEBTOR_ID, -1L)
        val daysBefore = inputData.getInt(KEY_DAYS_BEFORE, 1)
        if (debtorId <= 0L) return Result.failure()
        val app = applicationContext as MoneyApplication
        DebtReminderPresenter.showAdvance(app, debtorId, daysBefore)
        return Result.success()
    }

    companion object {
        const val KEY_DEBTOR_ID = "debtor_id"
        const val KEY_DAYS_BEFORE = "days_before"

        fun uniqueWorkName(debtorId: Long, daysBefore: Int): String =
            "debt_advance_${debtorId}_$daysBefore"
    }
}
