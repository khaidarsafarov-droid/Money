package com.example.money.domain.util

import com.example.money.domain.model.SavingsGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class GoalProgressTest {

    @Test
    fun progressPercent_allowsOverOneHundred() {
        assertEquals(125, GoalProgress.progressPercent(10_000, 12_500))
    }

    @Test
    fun displayProgressFraction_capsBarAtOne() {
        assertEquals(1f, GoalProgress.displayProgressFraction(10_000, 15_000))
    }

    @Test
    fun dailyGoalCents_dividesRemainingByDays() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 1, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val deadline = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 11, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val daily = GoalProgress.dailyGoalCents(
            remainingCents = 10_000,
            deadlineMillis = deadline,
            now = now,
        )
        val days = GoalProgress.daysUntilDeadline(deadline, now)!!

        assertEquals(kotlin.math.ceil(10_000.0 / days).toLong(), daily)
    }

    @Test
    fun dailyGoalCents_nullWhenNoDeadline() {
        assertNull(GoalProgress.dailyGoalCents(5_000, null))
    }

    @Test
    fun isOverAchieved_whenCurrentExceedsTarget() {
        assertTrue(GoalProgress.isOverAchieved(10_000, 12_000))
        assertFalse(GoalProgress.isOverAchieved(10_000, 10_000))
    }

    @Test
    fun orderedForSection_putsOverdueThenSoonestThenOpenThenDone() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 1, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val overdue = goal("Overdue", current = 100, target = 1_000, deadline = day(2026, Calendar.MAY, 1))
        val soon = goal("Soon", current = 100, target = 1_000, deadline = day(2026, Calendar.JUNE, 10))
        val later = goal("Later", current = 100, target = 1_000, deadline = day(2026, Calendar.JULY, 1))
        val open = goal("Open", current = 100, target = 1_000)
        val done = goal("Done", current = 1_000, target = 1_000, deadline = day(2026, Calendar.JUNE, 2))

        val ordered = GoalProgress.orderedForSection(listOf(done, open, later, soon, overdue), now)

        assertEquals(listOf("Overdue", "Soon", "Later", "Open", "Done"), ordered.map { it.name })
    }

    @Test
    fun summarize_totalsActiveGoalsAndDailyPace() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 1, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val active = goal("Trip", current = 2_000, target = 10_000, deadline = day(2026, Calendar.JUNE, 11))
        val done = goal("Done", current = 5_000, target = 5_000)
        val summary = GoalProgress.summarize(listOf(active, done), now)

        assertEquals(1, summary.activeCount)
        assertEquals(2_000, summary.savedCents)
        assertEquals(10_000, summary.targetCents)
        assertEquals(20, summary.progressPercent)
        val days = GoalProgress.daysUntilDeadline(active.deadlineMillis!!, now)!!
        assertEquals(kotlin.math.ceil(8_000.0 / days).toLong(), summary.dailyCents)
        assertEquals(0, summary.overdueCount)
    }

    private fun day(year: Int, month: Int, dayOfMonth: Int): Long =
        Calendar.getInstance().apply {
            set(year, month, dayOfMonth, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun goal(name: String, current: Long, target: Long, deadline: Long? = null) = SavingsGoal(
        name = name,
        targetAmountCents = target,
        currentAmountCents = current,
        updatedAt = 0L,
        deadlineMillis = deadline,
    )
}
