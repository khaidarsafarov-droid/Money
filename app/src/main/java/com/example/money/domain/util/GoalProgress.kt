package com.example.money.domain.util

import kotlin.math.ceil
import kotlin.math.max

object GoalProgress {

    private const val DAY_MS = 86_400_000L

    fun progressFractionRaw(targetCents: Long, currentCents: Long): Float {
        if (targetCents <= 0L) return 0f
        return currentCents.toFloat() / targetCents.toFloat()
    }

    /** Bar width stays at 100% when the goal is exceeded. */
    fun displayProgressFraction(targetCents: Long, currentCents: Long): Float =
        progressFractionRaw(targetCents, currentCents).coerceIn(0f, 1f)

    fun progressPercent(targetCents: Long, currentCents: Long): Int =
        (progressFractionRaw(targetCents, currentCents) * 100f).toInt()

    fun isOverAchieved(targetCents: Long, currentCents: Long): Boolean =
        targetCents > 0L && currentCents > targetCents

    fun surplusCents(targetCents: Long, currentCents: Long): Long =
        max(currentCents - targetCents, 0L)

    /** Remaining cents until target; zero when already met or exceeded. */
    fun dailyGoalCents(
        remainingCents: Long,
        deadlineMillis: Long?,
        now: Long = System.currentTimeMillis(),
    ): Long? {
        if (deadlineMillis == null || remainingCents <= 0L) return null

        val daysLeft = daysUntilDeadline(deadlineMillis, now) ?: return null
        return ceil(remainingCents.toDouble() / daysLeft.toDouble()).toLong()
    }

    fun daysUntilDeadline(deadlineMillis: Long, now: Long = System.currentTimeMillis()): Int? {
        val endOfDeadlineDay = endOfDayMillis(deadlineMillis)
        val diff = endOfDeadlineDay - startOfDayMillis(now)
        if (diff < 0L) return null
        return max(1, ceil(diff.toDouble() / DAY_MS.toDouble()).toInt())
    }

    fun isDeadlinePassed(deadlineMillis: Long?, now: Long = System.currentTimeMillis()): Boolean {
        if (deadlineMillis == null) return false
        return endOfDayMillis(deadlineMillis) < now
    }

    private fun startOfDayMillis(millis: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = millis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun endOfDayMillis(millis: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = millis
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }
}
