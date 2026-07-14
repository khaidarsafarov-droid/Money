package com.example.money.notification

import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DebtReminderSchedulerTest {

    @Test
    fun delayUntilNextDailyReminder_targetsNineAm() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JULY)
            set(Calendar.DAY_OF_MONTH, 5)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = calendar.timeInMillis

        val delay = DebtReminderScheduler.delayUntilNextDailyReminder(now)
        val next = Calendar.getInstance().apply { timeInMillis = now + delay }

        assertTrue(next.get(Calendar.HOUR_OF_DAY) == DebtReminderScheduler.OVERDUE_NOTIFICATION_HOUR)
        assertTrue(next.get(Calendar.MINUTE) == 0)
        assertTrue(next.get(Calendar.DAY_OF_MONTH) == 6)
    }

    @Test
    fun delayUntilNextDailyReminder_sameDayBeforeNineAm() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JULY)
            set(Calendar.DAY_OF_MONTH, 5)
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = calendar.timeInMillis

        val delay = DebtReminderScheduler.delayUntilNextDailyReminder(now)
        val next = Calendar.getInstance().apply { timeInMillis = now + delay }

        assertTrue(next.get(Calendar.HOUR_OF_DAY) == DebtReminderScheduler.OVERDUE_NOTIFICATION_HOUR)
        assertTrue(next.get(Calendar.DAY_OF_MONTH) == 5)
    }

    @Test
    fun delayUntilDueDateReminder_usesNineAmOnDueDay() {
        val dueAtNoon = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JULY)
            set(Calendar.DAY_OF_MONTH, 10)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val now = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JULY)
            set(Calendar.DAY_OF_MONTH, 9)
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val delay = DebtReminderScheduler.delayUntilDueDateReminder(dueAtNoon, now)
        val fireAt = Calendar.getInstance().apply { timeInMillis = now + delay }

        assertTrue(fireAt.get(Calendar.DAY_OF_MONTH) == 10)
        assertTrue(fireAt.get(Calendar.HOUR_OF_DAY) == DebtReminderScheduler.OVERDUE_NOTIFICATION_HOUR)
    }

    @Test
    fun delayUntilDueDateReminder_afterDueDayFallsBackToDailyNineAm() {
        val dueAtNoon = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JULY)
            set(Calendar.DAY_OF_MONTH, 10)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val now = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JULY)
            set(Calendar.DAY_OF_MONTH, 10)
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val delay = DebtReminderScheduler.delayUntilDueDateReminder(dueAtNoon, now)
        val fireAt = Calendar.getInstance().apply { timeInMillis = now + delay }

        assertTrue(fireAt.get(Calendar.HOUR_OF_DAY) == DebtReminderScheduler.OVERDUE_NOTIFICATION_HOUR)
        assertTrue(fireAt.get(Calendar.DAY_OF_MONTH) == 11)
    }
}
