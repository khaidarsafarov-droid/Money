package com.example.money.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.money.MoneyApplication
import com.example.money.data.mapper.toDomain

class DebtReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val debtorId = inputData.getLong(KEY_DEBTOR_ID, -1L)
        if (debtorId == -1L) return Result.failure()

        val app = applicationContext as MoneyApplication
        val debtorEntity = app.database.debtorDao().getDebtorById(debtorId)
            ?: return Result.success()

        if (debtorEntity.status == "COMPLETED") return Result.success()

        val debtor = debtorEntity.toDomain()
        val now = System.currentTimeMillis()
        val dueAtNine = DebtReminderScheduler.dueDateAtNineAm(debtor.dateDue)
        if (now < dueAtNine) {
            app.debtReminderScheduler.schedule(debtor)
            return Result.success()
        }

        DebtNotificationHelper.show(applicationContext, debtor)

        if (!debtor.isPaid) {
            app.debtReminderScheduler.scheduleDailyFollowUp(debtor)
        }
        return Result.success()
    }

    companion object {
        const val KEY_DEBTOR_ID = "debtor_id"
        const val KEY_DEBTOR_NAME = "debtor_name"
        const val KEY_AMOUNT = "amount"

        fun uniqueWorkName(debtorId: Long): String = "debt_reminder_$debtorId"
    }
}
