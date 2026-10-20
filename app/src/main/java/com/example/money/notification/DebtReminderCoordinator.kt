package com.example.money.notification

import com.example.money.domain.model.Debtor
import com.example.money.settings.PersonalBranchSettings

/**
 * Schedules advance reminders only when debtor data or notification settings change.
 * Avoids re-enqueueing WorkManager jobs on every Flow emission (battery + I/O).
 */
object DebtReminderCoordinator {

    data class ReminderSignature(
        val dateDue: Long,
        val notificationsEnabled: Boolean,
        val remind3Days: Boolean,
        val remind1Day: Boolean,
        val reminderHour: Int,
        val reminderMinute: Int,
    ) {
        companion object {
            fun from(debtor: Debtor, settings: PersonalBranchSettings) = ReminderSignature(
                dateDue = debtor.dateDue,
                notificationsEnabled = settings.notificationsEnabledFor(debtor.mode),
                remind3Days = settings.debtAdvanceRemind3Days,
                remind1Day = settings.debtAdvanceRemind1Day,
                reminderHour = settings.reminderHour,
                reminderMinute = settings.reminderMinute,
            )
        }
    }

    fun syncAdvanceReminders(
        scheduler: DebtReminderScheduler,
        settings: PersonalBranchSettings,
        previous: Map<Long, ReminderSignature>,
        debtors: List<Debtor>,
    ): Map<Long, ReminderSignature> {
        val active = debtors.filter { !it.isPaid && it.id != 0L }
        val activeIds = active.map { it.id }.toSet()

        previous.keys
            .filter { it !in activeIds }
            .forEach(scheduler::cancel)

        val next = LinkedHashMap<Long, ReminderSignature>(active.size)
        active.forEach { debtor ->
            val signature = ReminderSignature.from(debtor, settings)
            if (previous[debtor.id] != signature) {
                if (!signature.notificationsEnabled) {
                    scheduler.cancel(debtor.id)
                } else {
                    scheduler.schedule(
                        debtor,
                        reminderHour = settings.reminderHour,
                        reminderMinute = settings.reminderMinute,
                        preserveSnooze = false,
                    )
                    scheduler.scheduleAdvanceReminders(debtor, settings)
                }
            }
            next[debtor.id] = signature
        }
        return next
    }
}
