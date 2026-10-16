package com.example.money.ui.goal

import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class IsoWeekKeyTest {

    @Test
    fun currentIsoWeekKey_usesWeekBasedYear() {
        val newYearsEve = Instant.parse("2025-12-31T12:00:00Z").toEpochMilli()
        val newYearsDay = Instant.parse("2026-01-01T12:00:00Z").toEpochMilli()

        assertEquals("2026-W1", GoalHubViewModel.currentIsoWeekKey(newYearsEve, ZoneOffset.UTC))
        assertEquals("2026-W1", GoalHubViewModel.currentIsoWeekKey(newYearsDay, ZoneOffset.UTC))
    }
}
