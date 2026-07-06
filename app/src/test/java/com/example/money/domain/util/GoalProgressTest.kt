package com.example.money.domain.util

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
}
