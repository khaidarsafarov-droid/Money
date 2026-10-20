package com.example.money.notification

import com.example.money.MoneyApplication
import com.example.money.data.mapper.toDomain
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.first

object DebtReminderPresenter {

    private val inFlight = ConcurrentHashMap.newKeySet<String>()

    suspend fun showDue(app: MoneyApplication, debtorId: Long, snooze: Boolean) {
        if (!inFlight.add("due:$debtorId")) return
        val reschedule = try {
            showDueLocked(app, debtorId, snooze)
        } finally {
            inFlight.remove("due:$debtorId")
        }
        reschedule?.let { pending ->
            app.debtReminderScheduler.schedule(
                pending.debtor,
                reminderHour = pending.hour,
                reminderMinute = pending.minute,
                preserveSnooze = true,
            )
        }
    }

    private data class PendingSchedule(
        val debtor: com.example.money.domain.model.Debtor,
        val hour: Int,
        val minute: Int,
    )

    private suspend fun showDueLocked(
        app: MoneyApplication,
        debtorId: Long,
        snooze: Boolean,
    ): PendingSchedule? {
        val entity = app.database.debtorDao().getDebtorById(debtorId) ?: return null
        val debtor = entity.toDomain()
        if (debtor.isPaid) {
            app.debtReminderScheduler.cancel(debtorId)
            return null
        }
        val settings = app.preferencesStore.personalBranchSettings.first()
        if (!settings.notificationsEnabledFor(debtor.mode)) {
            app.debtReminderScheduler.cancel(debtorId)
            return null
        }
        val now = System.currentTimeMillis()
        val dueAt = DebtReminderTiming.dueReminderAt(
            debtor.dateDue,
            settings.reminderHour,
            settings.reminderMinute,
        )
        if (!snooze && now < dueAt) {
            return PendingSchedule(debtor, settings.reminderHour, settings.reminderMinute)
        }
        val currency = app.preferencesStore.currencyRegion.first()
        val client = debtor.clientId?.let { id ->
            app.database.clientDao().getById(id, debtor.mode.name)
        }
        val shown = DebtNotificationHelper.show(
            context = app,
            debtor = debtor,
            currencyRegion = currency,
            clientName = client?.name,
            company = client?.company,
        )
        if (!shown) return null
        val state = DebtReminderState(app)
        state.clearSnooze(debtorId)
        state.setLastShownDayStart(debtorId, DebtReminderTiming.startOfDay(now))
        if (now < dueAt) {
            app.debtReminderScheduler.schedule(
                debtor,
                reminderHour = settings.reminderHour,
                reminderMinute = settings.reminderMinute,
                preserveSnooze = false,
            )
        } else {
            app.debtReminderScheduler.scheduleDailyFollowUp(
                debtor,
                reminderHour = settings.reminderHour,
                reminderMinute = settings.reminderMinute,
            )
        }
        return null
    }

    suspend fun showAdvance(app: MoneyApplication, debtorId: Long, daysBefore: Int) {
        if (!inFlight.add("advance:$debtorId:$daysBefore")) return
        try {
            showAdvanceLocked(app, debtorId, daysBefore)
        } finally {
            inFlight.remove("advance:$debtorId:$daysBefore")
        }
    }

    private suspend fun showAdvanceLocked(app: MoneyApplication, debtorId: Long, daysBefore: Int) {
        val entity = app.database.debtorDao().getDebtorById(debtorId) ?: return
        val debtor = entity.toDomain()
        if (debtor.isPaid) return
        val settings = app.preferencesStore.personalBranchSettings.first()
        if (!settings.notificationsEnabledFor(debtor.mode)) return
        val enabled = when (daysBefore) {
            3 -> settings.debtAdvanceRemind3Days
            1 -> settings.debtAdvanceRemind1Day
            else -> false
        }
        if (!enabled) return
        val now = System.currentTimeMillis()
        val advanceAt = DebtReminderTiming.advanceReminderAt(
            debtor.dateDue,
            daysBefore,
            settings.reminderHour,
            settings.reminderMinute,
        )
        if (now < advanceAt) {
            app.debtReminderScheduler.scheduleAdvanceReminders(debtor, settings)
            return
        }
        val currency = app.preferencesStore.currencyRegion.first()
        val client = debtor.clientId?.let { id ->
            app.database.clientDao().getById(id, debtor.mode.name)
        }
        DebtNotificationHelper.showAdvanceReminder(
            context = app,
            debtor = debtor,
            daysBefore = daysBefore,
            currencyRegion = currency,
            clientName = client?.name,
            company = client?.company,
        )
    }
}
