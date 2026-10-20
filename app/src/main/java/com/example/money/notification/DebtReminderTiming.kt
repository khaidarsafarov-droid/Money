package com.example.money.notification

import java.util.Calendar

/**
 * Pure local-time math for debt reminders. Quiet hours apply to snooze landing
 * times only; a user-chosen reminder hour inside 22:00–08:00 is still honored.
 */
object DebtReminderTiming {
    const val DEFAULT_REMINDER_HOUR = 9
    const val DEFAULT_REMINDER_MINUTE = 0
    const val QUIET_START_HOUR = 22
    const val QUIET_END_HOUR = 8

    fun dueReminderAt(
        dueMillis: Long,
        hour: Int = DEFAULT_REMINDER_HOUR,
        minute: Int = DEFAULT_REMINDER_MINUTE,
    ): Long = atTime(dueMillis, hour, minute)

    fun delayUntilDueDateReminder(
        dueMillis: Long,
        now: Long,
        hour: Int = DEFAULT_REMINDER_HOUR,
        minute: Int = DEFAULT_REMINDER_MINUTE,
        lastShownDayStart: Long? = null,
    ): Long {
        val dueAt = dueReminderAt(dueMillis, hour, minute)
        if (dueAt > now) return dueAt - now
        val todayStart = startOfDay(now)
        if (lastShownDayStart != null && lastShownDayStart == todayStart) {
            return delayUntilNextDailyReminder(now, hour, minute)
        }
        val todayAt = atTime(now, hour, minute)
        if (todayAt > now) return todayAt - now
        return 0L
    }

    fun delayUntilNextDailyReminder(
        now: Long,
        hour: Int = DEFAULT_REMINDER_HOUR,
        minute: Int = DEFAULT_REMINDER_MINUTE,
    ): Long {
        val todayAt = atTime(now, hour, minute)
        if (todayAt > now) return todayAt - now
        val next = Calendar.getInstance().apply {
            timeInMillis = todayAt
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis - now
    }

    fun advanceReminderAt(
        dueMillis: Long,
        daysBefore: Int,
        hour: Int = DEFAULT_REMINDER_HOUR,
        minute: Int = DEFAULT_REMINDER_MINUTE,
    ): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dueMillis
            add(Calendar.DAY_OF_YEAR, -daysBefore)
        }
        return atTime(calendar.timeInMillis, hour, minute)
    }

    fun delayUntilAdvanceReminder(
        dueMillis: Long,
        daysBefore: Int,
        now: Long,
        hour: Int = DEFAULT_REMINDER_HOUR,
        minute: Int = DEFAULT_REMINDER_MINUTE,
    ): Long? {
        val target = advanceReminderAt(dueMillis, daysBefore, hour, minute)
        return if (target > now) target - now else null
    }

    fun adjustSnoozeFireAt(
        requestedAt: Long,
        reminderHour: Int,
        reminderMinute: Int,
    ): Long {
        val hourOfDay = Calendar.getInstance().apply { timeInMillis = requestedAt }
            .get(Calendar.HOUR_OF_DAY)
        if (!isQuietHour(hourOfDay)) return requestedAt
        val snapped = atTime(requestedAt, reminderHour, reminderMinute)
        if (snapped > requestedAt) return snapped
        return Calendar.getInstance().apply {
            timeInMillis = snapped
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    }

    fun isQuietHour(hourOfDay: Int): Boolean =
        hourOfDay >= QUIET_START_HOUR || hourOfDay < QUIET_END_HOUR

    fun startOfDay(millis: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun atTime(dayMillis: Long, hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = dayMillis }
        calendar.set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        calendar.set(Calendar.MINUTE, minute.coerceIn(0, 59))
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
