package com.example.money.domain.util

import com.example.money.domain.model.GoalKind
import com.example.money.domain.model.SavingsGoal
import kotlin.math.ceil
import kotlin.math.max

data class GoalSectionSummary(
    val activeCount: Int,
    val savedCents: Long,
    val targetCents: Long,
    val dailyCents: Long?,
    val overdueCount: Int,
) {
    val progressPercent: Int
        get() = GoalProgress.progressPercent(targetCents, savedCents)
}

object GoalProgress {

    private const val DAY_MS = 86_400_000L

    fun progressFractionRaw(targetCents: Long, currentCents: Long): Float {
        if (targetCents <= 0L) return 0f
        return currentCents.toFloat() / targetCents.toFloat()
    }

    /** Bar width stays at 100% when the goal is exceeded. */
    fun displayProgressFraction(targetCents: Long, currentCents: Long): Float =
        progressFractionRaw(targetCents, currentCents).coerceIn(0f, 1f)

    fun progressPercent(targetCents: Long, currentCents: Long): Int {
        if (targetCents <= 0L) return 0
        return (currentCents * 100L / targetCents).toInt()
    }

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

    fun orderedForSection(
        goals: List<SavingsGoal>,
        now: Long = System.currentTimeMillis(),
    ): List<SavingsGoal> =
        goals.sortedWith(
            compareBy<SavingsGoal> { sectionRank(it, now) }
                .thenBy { it.deadlineMillis ?: Long.MAX_VALUE }
                .thenBy { it.name.lowercase() },
        )

    fun summarize(
        goals: List<SavingsGoal>,
        now: Long = System.currentTimeMillis(),
    ): GoalSectionSummary {
        val moneyGoals = goals.filter { it.kind == GoalKind.MONEY }
        val active = moneyGoals.filter { !it.isCompleted }
        val dailies = active.mapNotNull { goal ->
            dailyGoalCents(goal.remainingAmountCents, goal.deadlineMillis, now)
        }
        val overdueCount = active.count { goal ->
            isDeadlinePassed(goal.deadlineMillis, now)
        }
        return GoalSectionSummary(
            activeCount = active.size,
            savedCents = active.sumOf { it.currentAmountCents },
            targetCents = active.sumOf { it.targetAmountCents },
            dailyCents = dailies.takeIf { it.isNotEmpty() }?.sum(),
            overdueCount = overdueCount,
        )
    }

    private fun sectionRank(goal: SavingsGoal, now: Long): Int {
        if (goal.isCompleted) return 3
        val deadline = goal.deadlineMillis ?: return 2
        if (isDeadlinePassed(deadline, now)) return 0
        return 1
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
