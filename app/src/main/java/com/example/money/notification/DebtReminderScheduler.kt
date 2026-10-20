package com.example.money.notification

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.money.MoneyApplication
import com.example.money.domain.model.Debtor
import com.example.money.settings.PersonalBranchSettings
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface DebtReminderScheduler {

    fun schedule(
        debtor: Debtor,
        reminderHour: Int = DebtReminderTiming.DEFAULT_REMINDER_HOUR,
        reminderMinute: Int = DebtReminderTiming.DEFAULT_REMINDER_MINUTE,
        preserveSnooze: Boolean = false,
    )

    fun showNow(debtor: Debtor): Boolean

    fun snooze(
        debtor: Debtor,
        delayMillis: Long = SNOOZE_DELAY_MS,
        reminderHour: Int = DebtReminderTiming.DEFAULT_REMINDER_HOUR,
        reminderMinute: Int = DebtReminderTiming.DEFAULT_REMINDER_MINUTE,
    )

    fun cancel(debtorId: Long)

    suspend fun cancelAll()

    fun scheduleDailyFollowUp(
        debtor: Debtor,
        reminderHour: Int = DebtReminderTiming.DEFAULT_REMINDER_HOUR,
        reminderMinute: Int = DebtReminderTiming.DEFAULT_REMINDER_MINUTE,
    )

    fun scheduleAdvanceReminders(debtor: Debtor, settings: PersonalBranchSettings)

    fun cancelAdvanceReminders(debtorId: Long)

    companion object {
        val SNOOZE_DELAY_MS: Long = TimeUnit.HOURS.toMillis(1)
        const val OVERDUE_NOTIFICATION_HOUR = DebtReminderTiming.DEFAULT_REMINDER_HOUR
        const val WORK_TAG = "debt_reminder_work"

        fun dueDateAtNineAm(dueMillis: Long): Long =
            DebtReminderTiming.dueReminderAt(dueMillis)

        fun delayUntilDueDateReminder(
            dueMillis: Long,
            now: Long = System.currentTimeMillis(),
            hour: Int = OVERDUE_NOTIFICATION_HOUR,
            minute: Int = DebtReminderTiming.DEFAULT_REMINDER_MINUTE,
            lastShownDayStart: Long? = null,
        ): Long = DebtReminderTiming.delayUntilDueDateReminder(
            dueMillis,
            now,
            hour,
            minute,
            lastShownDayStart,
        )

        fun delayUntilNextDailyReminder(
            now: Long = System.currentTimeMillis(),
            hour: Int = OVERDUE_NOTIFICATION_HOUR,
            minute: Int = DebtReminderTiming.DEFAULT_REMINDER_MINUTE,
        ): Long = DebtReminderTiming.delayUntilNextDailyReminder(now, hour, minute)

        fun advanceReminderAt(
            dueMillis: Long,
            daysBefore: Int,
            hour: Int = OVERDUE_NOTIFICATION_HOUR,
            minute: Int = DebtReminderTiming.DEFAULT_REMINDER_MINUTE,
        ): Long = DebtReminderTiming.advanceReminderAt(dueMillis, daysBefore, hour, minute)

        fun delayUntilAdvanceReminder(
            dueMillis: Long,
            daysBefore: Int,
            now: Long = System.currentTimeMillis(),
            hour: Int = OVERDUE_NOTIFICATION_HOUR,
            minute: Int = DebtReminderTiming.DEFAULT_REMINDER_MINUTE,
        ): Long? = DebtReminderTiming.delayUntilAdvanceReminder(
            dueMillis,
            daysBefore,
            now,
            hour,
            minute,
        )
    }
}

class DebtReminderSchedulerImpl(
    private val context: Context,
) : DebtReminderScheduler {

    private val workManager = WorkManager.getInstance(context)
    private val state = DebtReminderState(context)

    override fun schedule(
        debtor: Debtor,
        reminderHour: Int,
        reminderMinute: Int,
        preserveSnooze: Boolean,
    ) {
        if (debtor.isPaid || debtor.id == 0L) return
        try {
            val hour = reminderHour.coerceIn(0, 23)
            val minute = reminderMinute.coerceIn(0, 59)
            val now = System.currentTimeMillis()
            if (preserveSnooze) {
                val snoozeAt = state.snoozeFireAt(debtor.id)
                if (snoozeAt != null) {
                    armDue(debtor, snoozeAt, snooze = true)
                    return
                }
            } else {
                state.clearSnooze(debtor.id)
            }
            val delay = DebtReminderTiming.delayUntilDueDateReminder(
                dueMillis = debtor.dateDue,
                now = now,
                hour = hour,
                minute = minute,
                lastShownDayStart = state.lastShownDayStart(debtor.id),
            )
            armDue(debtor, now + delay, snooze = false)
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

    override fun snooze(
        debtor: Debtor,
        delayMillis: Long,
        reminderHour: Int,
        reminderMinute: Int,
    ) {
        if (debtor.isPaid || debtor.id == 0L) return
        val requestedAt = System.currentTimeMillis() + delayMillis
        val fireAt = DebtReminderTiming.adjustSnoozeFireAt(
            requestedAt,
            reminderHour.coerceIn(0, 23),
            reminderMinute.coerceIn(0, 59),
        )
        state.setSnoozeFireAt(debtor.id, fireAt)
        armDue(debtor, fireAt, snooze = true)
    }

    override fun cancel(debtorId: Long) {
        DebtReminderAlarms.cancelDue(context, debtorId)
        workManager.cancelUniqueWork(DebtReminderWorker.uniqueWorkName(debtorId))
        cancelAdvanceReminders(debtorId)
        state.clear(debtorId)
        DebtNotificationHelper.cancel(context, debtorId)
    }

    override suspend fun cancelAll() {
        val tracked = state.trackedIds()
        tracked.forEach { debtorId ->
            DebtReminderAlarms.cancelDue(context, debtorId)
            DebtReminderAlarms.cancelAdvance(context, debtorId, 1)
            DebtReminderAlarms.cancelAdvance(context, debtorId, 3)
        }
        state.clearAll()
        try {
            withContext(Dispatchers.IO) {
                workManager.cancelAllWorkByTag(DebtReminderScheduler.WORK_TAG).result.get()
            }
        } finally {
            DebtNotificationHelper.cancelAll(context)
        }
    }

    override fun scheduleAdvanceReminders(debtor: Debtor, settings: PersonalBranchSettings) {
        if (debtor.isPaid || debtor.id == 0L) return
        if (!settings.notificationsEnabledFor(debtor.mode)) {
            cancelAdvanceReminders(debtor.id)
            return
        }
        cancelAdvanceReminders(debtor.id)
        val now = System.currentTimeMillis()
        if (settings.debtAdvanceRemind3Days) {
            scheduleAdvance(debtor, daysBefore = 3, now = now, settings = settings)
        }
        if (settings.debtAdvanceRemind1Day) {
            scheduleAdvance(debtor, daysBefore = 1, now = now, settings = settings)
        }
    }

    override fun cancelAdvanceReminders(debtorId: Long) {
        listOf(1, 3).forEach { daysBefore ->
            DebtReminderAlarms.cancelAdvance(context, debtorId, daysBefore)
            workManager.cancelUniqueWork(DebtAdvanceReminderWorker.uniqueWorkName(debtorId, daysBefore))
        }
    }

    override fun scheduleDailyFollowUp(
        debtor: Debtor,
        reminderHour: Int,
        reminderMinute: Int,
    ) {
        if (debtor.isPaid || debtor.id == 0L) return
        state.clearSnooze(debtor.id)
        val now = System.currentTimeMillis()
        val delay = DebtReminderTiming.delayUntilNextDailyReminder(
            now,
            reminderHour.coerceIn(0, 23),
            reminderMinute.coerceIn(0, 59),
        )
        armDue(debtor, now + delay, snooze = false)
    }

    private fun scheduleAdvance(
        debtor: Debtor,
        daysBefore: Int,
        now: Long,
        settings: PersonalBranchSettings,
    ) {
        val delay = DebtReminderTiming.delayUntilAdvanceReminder(
            debtor.dateDue,
            daysBefore,
            now,
            settings.reminderHour,
            settings.reminderMinute,
        ) ?: return
        val triggerAt = now + delay
        state.track(debtor.id)
        if (DebtReminderAlarms.scheduleAdvance(context, debtor.id, daysBefore, triggerAt)) {
            workManager.cancelUniqueWork(DebtAdvanceReminderWorker.uniqueWorkName(debtor.id, daysBefore))
            return
        }
        val inputData = Data.Builder()
            .putLong(DebtAdvanceReminderWorker.KEY_DEBTOR_ID, debtor.id)
            .putInt(DebtAdvanceReminderWorker.KEY_DAYS_BEFORE, daysBefore)
            .build()
        val request = OneTimeWorkRequestBuilder<DebtAdvanceReminderWorker>()
            .setInitialDelay(delay.coerceIn(MIN_SCHEDULE_DELAY_MS, MAX_SCHEDULE_DELAY_MS), TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(DebtReminderScheduler.WORK_TAG)
            .build()
        workManager.enqueueUniqueWork(
            DebtAdvanceReminderWorker.uniqueWorkName(debtor.id, daysBefore),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun armDue(debtor: Debtor, triggerAt: Long, snooze: Boolean) {
        state.track(debtor.id)
        if (triggerAt <= System.currentTimeMillis()) {
            DebtReminderAlarms.cancelDue(context, debtor.id)
            workManager.cancelUniqueWork(DebtReminderWorker.uniqueWorkName(debtor.id))
            fireDueNow(debtor.id, snooze)
            return
        }
        if (DebtReminderAlarms.scheduleDue(context, debtor.id, triggerAt, snooze)) {
            workManager.cancelUniqueWork(DebtReminderWorker.uniqueWorkName(debtor.id))
            return
        }
        enqueueDueWork(debtor, triggerAt - System.currentTimeMillis(), snooze)
    }

    private fun fireDueNow(debtorId: Long, snooze: Boolean) {
        val app = context.applicationContext as? MoneyApplication ?: return
        app.applicationScope.launch {
            DebtReminderPresenter.showDue(app, debtorId, snooze)
        }
    }

    private fun enqueueDueWork(debtor: Debtor, delayMillis: Long, snooze: Boolean) {
        val safeDelayMillis = delayMillis.coerceIn(MIN_SCHEDULE_DELAY_MS, MAX_SCHEDULE_DELAY_MS)
        val inputData = Data.Builder()
            .putLong(DebtReminderWorker.KEY_DEBTOR_ID, debtor.id)
            .putString(DebtReminderWorker.KEY_DEBTOR_NAME, debtor.name)
            .putDouble(DebtReminderWorker.KEY_AMOUNT, debtor.amount)
            .putBoolean(DebtReminderWorker.KEY_SNOOZE, snooze)
            .build()
        val request = OneTimeWorkRequestBuilder<DebtReminderWorker>()
            .setInitialDelay(safeDelayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(DebtReminderScheduler.WORK_TAG)
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
