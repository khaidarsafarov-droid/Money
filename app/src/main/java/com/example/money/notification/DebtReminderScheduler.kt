package com.example.money.notification

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.money.domain.model.Debtor
import com.example.money.settings.PersonalBranchSettings
import java.util.Calendar
import java.util.concurrent.TimeUnit

interface DebtReminderScheduler {

    fun schedule(debtor: Debtor)

    fun showNow(debtor: Debtor): Boolean

    fun snooze(debtor: Debtor, delayMillis: Long = SNOOZE_DELAY_MS)

    fun cancel(debtorId: Long)

    fun scheduleDailyFollowUp(debtor: Debtor)

    fun scheduleAdvanceReminders(debtor: Debtor, settings: PersonalBranchSettings)

    fun cancelAdvanceReminders(debtorId: Long)

    companion object {
        val SNOOZE_DELAY_MS: Long = TimeUnit.HOURS.toMillis(1)
        const val OVERDUE_NOTIFICATION_HOUR = 9

        fun dueDateAtNineAm(dueMillis: Long): Long {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = dueMillis
                set(Calendar.HOUR_OF_DAY, OVERDUE_NOTIFICATION_HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return calendar.timeInMillis
        }

        fun delayUntilDueDateReminder(dueMillis: Long, now: Long = System.currentTimeMillis()): Long {
            val dueAtNine = dueDateAtNineAm(dueMillis)
            return if (dueAtNine > now) {
                dueAtNine - now
            } else {
                delayUntilNextDailyReminder(now)
            }
        }

        fun delayUntilNextDailyReminder(now: Long = System.currentTimeMillis()): Long {
            val calendar = Calendar.getInstance().apply { timeInMillis = now }
            calendar.set(Calendar.HOUR_OF_DAY, OVERDUE_NOTIFICATION_HOUR)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            if (calendar.timeInMillis <= now) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis - now
        }

        fun delayUntilAdvanceReminder(dueMillis: Long, daysBefore: Int, now: Long = System.currentTimeMillis()): Long? {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = dueMillis
                add(Calendar.DAY_OF_YEAR, -daysBefore)
                set(Calendar.HOUR_OF_DAY, OVERDUE_NOTIFICATION_HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val target = calendar.timeInMillis
            return if (target > now) target - now else null
        }
    }
}

class DebtReminderSchedulerImpl(
    private val context: Context,
) : DebtReminderScheduler {

    private val workManager = WorkManager.getInstance(context)

    override fun schedule(debtor: Debtor) {
        if (debtor.isPaid || debtor.id == 0L) return

        try {
            val now = System.currentTimeMillis()
            val delayUntilDue = DebtReminderScheduler.delayUntilDueDateReminder(debtor.dateDue, now)
            enqueueReminder(debtor, delayUntilDue)
        } catch (exception: Exception) {
            Log.w(TAG, "Failed to schedule reminder for debtor ${debtor.id}", exception)
        }
    }

    override fun showNow(debtor: Debtor): Boolean {
        if (debtor.isPaid || debtor.id == 0L) return false
        return try {
            DebtNotificationHelper.show(context, debtor)
        } catch (exception: Exception) {
            Log.w(TAG, "Failed to show reminder for debtor ${debtor.id}", exception)
            false
        }
    }

    override fun snooze(debtor: Debtor, delayMillis: Long) {
        if (debtor.isPaid || debtor.id == 0L) return
        enqueueReminder(debtor, delayMillis)
    }

    override fun cancel(debtorId: Long) {
        workManager.cancelUniqueWork(DebtReminderWorker.uniqueWorkName(debtorId))
        cancelAdvanceReminders(debtorId)
        DebtNotificationHelper.cancel(context, debtorId)
    }

    override fun scheduleAdvanceReminders(debtor: Debtor, settings: PersonalBranchSettings) {
        if (debtor.isPaid || debtor.id == 0L || !settings.debtNotificationsEnabled) return
        cancelAdvanceReminders(debtor.id)
        val now = System.currentTimeMillis()
        if (settings.debtAdvanceRemind3Days) {
            scheduleAdvance(debtor, daysBefore = 3, now)
        }
        if (settings.debtAdvanceRemind1Day) {
            scheduleAdvance(debtor, daysBefore = 1, now)
        }
    }

    override fun cancelAdvanceReminders(debtorId: Long) {
        workManager.cancelUniqueWork(DebtAdvanceReminderWorker.uniqueWorkName(debtorId, 1))
        workManager.cancelUniqueWork(DebtAdvanceReminderWorker.uniqueWorkName(debtorId, 3))
    }

    private fun scheduleAdvance(debtor: Debtor, daysBefore: Int, now: Long) {
        val delay = DebtReminderScheduler.delayUntilAdvanceReminder(debtor.dateDue, daysBefore, now)
            ?: return
        val inputData = Data.Builder()
            .putLong(DebtAdvanceReminderWorker.KEY_DEBTOR_ID, debtor.id)
            .putInt(DebtAdvanceReminderWorker.KEY_DAYS_BEFORE, daysBefore)
            .build()
        val request = OneTimeWorkRequestBuilder<DebtAdvanceReminderWorker>()
            .setInitialDelay(delay.coerceIn(MIN_SCHEDULE_DELAY_MS, MAX_SCHEDULE_DELAY_MS), TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()
        workManager.enqueueUniqueWork(
            DebtAdvanceReminderWorker.uniqueWorkName(debtor.id, daysBefore),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    override fun scheduleDailyFollowUp(debtor: Debtor) {
        if (debtor.isPaid || debtor.id == 0L) return
        enqueueReminder(debtor, DebtReminderScheduler.delayUntilNextDailyReminder())
    }

    private fun enqueueReminder(debtor: Debtor, delayMillis: Long) {
        val safeDelayMillis = delayMillis.coerceIn(
            MIN_SCHEDULE_DELAY_MS,
            MAX_SCHEDULE_DELAY_MS,
        )

        val inputData = Data.Builder()
            .putLong(DebtReminderWorker.KEY_DEBTOR_ID, debtor.id)
            .putString(DebtReminderWorker.KEY_DEBTOR_NAME, debtor.name)
            .putDouble(DebtReminderWorker.KEY_AMOUNT, debtor.amount)
            .build()

        val request = OneTimeWorkRequestBuilder<DebtReminderWorker>()
            .setInitialDelay(safeDelayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(DebtReminderWorker.uniqueWorkName(debtor.id))
            .build()

        workManager.enqueueUniqueWork(
            DebtReminderWorker.uniqueWorkName(debtor.id),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private companion object {
        const val TAG = "DebtReminderScheduler"
        val MIN_SCHEDULE_DELAY_MS: Long = TimeUnit.SECONDS.toMillis(1)
        val MAX_SCHEDULE_DELAY_MS: Long = TimeUnit.DAYS.toMillis(365)
    }
}
