package com.example.money.domain.util

import java.util.Calendar

/**
 * Due dates are stored at noon; overdue is evaluated at end-of-day so the full due date counts.
 */
object DebtDueDateUtils {

    fun startOfDayMillis(epochMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun endOfDayMillis(epochMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    /**
     * Room overdue filter cutoff: debts with `date_due < overdueBefore` are overdue.
     * Using start-of-today keeps the due day itself active until end-of-day
     * (matches [isOverdue] when due timestamps fall on the due calendar day).
     */
    fun overdueBeforeMillis(now: Long = System.currentTimeMillis()): Long = startOfDayMillis(now)

    fun isOverdue(dateDue: Long, now: Long = System.currentTimeMillis()): Boolean =
        now > endOfDayMillis(dateDue)

    fun isDueToday(dateDue: Long, now: Long = System.currentTimeMillis()): Boolean =
        dateDue > 0L && startOfDayMillis(dateDue) == startOfDayMillis(now)

    fun startPlusDays(now: Long, days: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = startOfDayMillis(now)
            add(Calendar.DAY_OF_YEAR, days)
        }
        return calendar.timeInMillis
    }

    fun isDueWithinDays(dateDue: Long, days: Int, now: Long = System.currentTimeMillis()): Boolean {
        if (dateDue <= 0L || isOverdue(dateDue, now)) return false
        return dateDue < startPlusDays(now, days)
    }
}
