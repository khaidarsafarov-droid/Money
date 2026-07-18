package com.example.money.domain.util

import java.util.Calendar

/**
 * Due dates are stored at noon; overdue is evaluated at end-of-day so the full due date counts.
 */
object DebtDueDateUtils {

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

    fun isOverdue(dateDue: Long, now: Long = System.currentTimeMillis()): Boolean =
        now > endOfDayMillis(dateDue)
}
