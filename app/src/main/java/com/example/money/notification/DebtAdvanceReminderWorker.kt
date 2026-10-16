package com.example.money.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.money.MoneyApplication
import com.example.money.data.mapper.toDomain
import com.example.money.domain.model.DebtType
import kotlinx.coroutines.flow.first

class DebtAdvanceReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val debtorId = inputData.getLong(KEY_DEBTOR_ID, -1L)
        val daysBefore = inputData.getInt(KEY_DAYS_BEFORE, 1)
        if (debtorId <= 0L) return Result.failure()

        val app = applicationContext as MoneyApplication
        val settings = app.preferencesStore.personalBranchSettings.first()
        val entity = app.database.debtorDao().getDebtorById(debtorId) ?: return Result.success()
        if (entity.status == "COMPLETED") return Result.success()

        val debtor = entity.toDomain()
        if (!settings.notificationsEnabledFor(debtor.mode)) return Result.success()
        if (debtor.debtType != DebtType.CREDITOR) return Result.success()

        val now = System.currentTimeMillis()
        val advanceAt = DebtReminderScheduler.advanceReminderAt(debtor.dateDue, daysBefore)
        if (now < advanceAt) {
            app.debtReminderScheduler.scheduleAdvanceReminders(debtor, settings)
            return Result.success()
        }

        val currency = app.preferencesStore.currencyRegion.first()
        val client = debtor.clientId?.let { id ->
            app.database.clientDao().getById(id, debtor.mode.name)
        }
        DebtNotificationHelper.showAdvanceReminder(
            context = applicationContext,
            debtor = debtor,
            daysBefore = daysBefore,
            currencyRegion = currency,
            clientName = client?.name,
            company = client?.company,
        )
        return Result.success()
    }

    companion object {
        const val KEY_DEBTOR_ID = "debtor_id"
        const val KEY_DAYS_BEFORE = "days_before"

        fun uniqueWorkName(debtorId: Long, daysBefore: Int): String =
            "debt_advance_${debtorId}_$daysBefore"
    }
}
