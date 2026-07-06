package com.example.money.voice

import com.example.money.domain.model.SavingsGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class VoiceReportComposerTest {

    @Test
    fun buildSnapshot_calculatesProgressFromActiveGoals() {
        val goals = listOf(
            SavingsGoal(
                id = 1L,
                name = "Truck",
                targetAmountCents = 100_000,
                currentAmountCents = 85_000,
                updatedAt = 0L,
            ),
            SavingsGoal(
                id = 2L,
                name = "Vacation",
                targetAmountCents = 50_000,
                currentAmountCents = 50_000,
                updatedAt = 0L,
            ),
        )

        val snapshot = VoiceReportComposer.buildSnapshot(goals)

        assertEquals(1, snapshot.activeGoalCount)
        assertEquals(1, snapshot.completedGoalCount)
        assertEquals(85_000, snapshot.totalCurrentCents)
        assertEquals(100_000, snapshot.totalTargetCents)
        assertEquals(15_000, snapshot.remainingCents)
        assertEquals(85, snapshot.progressPercent)
    }

    @Test
    fun computeDelayMillis_schedulesNextOccurrence() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 13, 7, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val delay = VoiceReportScheduler.computeDelayMillis(hour = 8, minute = 0, now = now)

        assertEquals(30 * 60 * 1000L, delay)
    }

    @Test
    fun computeDelayMillis_rollsToNextDayAfterTimePassed() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 13, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val delay = VoiceReportScheduler.computeDelayMillis(hour = 8, minute = 0, now = now)

        assertTrue(delay >= 23 * 60 * 60 * 1000L)
    }
}
