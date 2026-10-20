package com.example.money.notification

import org.junit.Assert.assertEquals
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
    fun delayUntilDueDateReminder_missedTodayFiresImmediately() {
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

        assertEquals(0L, delay)
    }

    @Test
    fun delayUntilDueDateReminder_shownTodayWaitsUntilTomorrow() {
        val dueAtNoon = at(2026, Calendar.JULY, 10, 12)
        val now = at(2026, Calendar.JULY, 10, 15)
        val shownToday = DebtReminderTiming.startOfDay(now)

        val delay = DebtReminderTiming.delayUntilDueDateReminder(
            dueMillis = dueAtNoon,
            now = now,
            lastShownDayStart = shownToday,
        )
        val fireAt = Calendar.getInstance().apply { timeInMillis = now + delay }

        assertEquals(11, fireAt.get(Calendar.DAY_OF_MONTH))
        assertEquals(DebtReminderTiming.DEFAULT_REMINDER_HOUR, fireAt.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun delayUntilDueDateReminder_beforeTodayHourWaitsForThatHour() {
        val dueYesterday = at(2026, Calendar.JULY, 9, 12)
        val now = at(2026, Calendar.JULY, 10, 8)

        val delay = DebtReminderTiming.delayUntilDueDateReminder(dueYesterday, now)
        val fireAt = Calendar.getInstance().apply { timeInMillis = now + delay }

        assertEquals(10, fireAt.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, fireAt.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun delayUntilDueDateReminder_customHour() {
        val due = at(2026, Calendar.JULY, 10, 12)
        val now = at(2026, Calendar.JULY, 10, 15)

        val delay = DebtReminderTiming.delayUntilDueDateReminder(due, now, hour = 18, minute = 30)
        val fireAt = Calendar.getInstance().apply { timeInMillis = now + delay }

        assertEquals(10, fireAt.get(Calendar.DAY_OF_MONTH))
        assertEquals(18, fireAt.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, fireAt.get(Calendar.MINUTE))
    }

    @Test
    fun advanceReminderUsesConfiguredHour() {
        val due = at(2026, Calendar.JULY, 10, 12)
        val fireAt = Calendar.getInstance().apply {
            timeInMillis = DebtReminderTiming.advanceReminderAt(due, daysBefore = 3, hour = 18, minute = 15)
        }

        assertEquals(7, fireAt.get(Calendar.DAY_OF_MONTH))
        assertEquals(18, fireAt.get(Calendar.HOUR_OF_DAY))
        assertEquals(15, fireAt.get(Calendar.MINUTE))
    }

    @Test
    fun snoozeDuringQuietHoursMovesToReminderHour() {
        val late = at(2026, Calendar.JULY, 10, 23, 30)
        val early = at(2026, Calendar.JULY, 10, 7, 0)
        val daytime = at(2026, Calendar.JULY, 10, 21, 30)

        val fromLate = Calendar.getInstance().apply {
            timeInMillis = DebtReminderTiming.adjustSnoozeFireAt(late, 9, 0)
        }
        val fromEarly = Calendar.getInstance().apply {
            timeInMillis = DebtReminderTiming.adjustSnoozeFireAt(early, 9, 0)
        }

        assertEquals(11, fromLate.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, fromLate.get(Calendar.HOUR_OF_DAY))
        assertEquals(10, fromEarly.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, fromEarly.get(Calendar.HOUR_OF_DAY))
        assertEquals(daytime, DebtReminderTiming.adjustSnoozeFireAt(daytime, 9, 0))
    }

    @Test
    fun snoozeHonorsReminderHourInsideQuietWindow() {
        val late = at(2026, Calendar.JULY, 10, 23, 30)
        val fireAt = Calendar.getInstance().apply {
            timeInMillis = DebtReminderTiming.adjustSnoozeFireAt(late, reminderHour = 23, reminderMinute = 0)
        }

        assertEquals(11, fireAt.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, fireAt.get(Calendar.HOUR_OF_DAY))
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
