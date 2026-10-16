package com.example.money.domain.util

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetPeriodsTest {

    private val newYork = ZoneId.of("America/New_York")

    @Test
    fun monthContaining_isHalfOpenOnLocalMonthBoundaries() {
        val march = BudgetPeriods.monthContaining(Instant.parse("2026-03-15T15:00:00Z"), newYork)

        assertEquals(LocalDate.of(2026, 3, 1), march.startInclusive.atZone(newYork).toLocalDate())
        assertEquals(LocalDate.of(2026, 4, 1), march.endExclusive.atZone(newYork).toLocalDate())
        assertTrue(march.contains(march.startMillis))
        assertFalse(march.contains(march.endMillis))
    }

    @Test
    fun weekContaining_startsOnPreviousOrSameFirstDay() {
        val sunday = Instant.parse("2026-03-08T16:00:00Z")
        val week = BudgetPeriods.weekContaining(sunday, newYork, DayOfWeek.MONDAY)

        assertEquals(LocalDate.of(2026, 3, 2), week.startInclusive.atZone(newYork).toLocalDate())
        assertEquals(LocalDate.of(2026, 3, 9), week.endExclusive.atZone(newYork).toLocalDate())
        assertTrue(week.contains(week.startMillis))
        assertFalse(week.contains(week.endMillis))
    }

    @Test
    fun dst_springForwardShortensMarchAndThatWeek() {
        val march = BudgetPeriods.monthContaining(Instant.parse("2026-03-15T15:00:00Z"), newYork)
        val week = BudgetPeriods.weekContaining(
            Instant.parse("2026-03-08T16:00:00Z"),
            newYork,
            DayOfWeek.MONDAY,
        )

        assertEquals(743L, Duration.between(march.startInclusive, march.endExclusive).toHours())
        assertEquals(167L, Duration.between(week.startInclusive, week.endExclusive).toHours())
    }

    @Test
    fun dst_fallBackLengthensNovemberAndThatWeek() {
        val november = BudgetPeriods.monthContaining(Instant.parse("2026-11-15T15:00:00Z"), newYork)
        val week = BudgetPeriods.weekContaining(
            Instant.parse("2026-11-01T16:00:00Z"),
            newYork,
            DayOfWeek.MONDAY,
        )

        assertEquals(721L, Duration.between(november.startInclusive, november.endExclusive).toHours())
        assertEquals(169L, Duration.between(week.startInclusive, week.endExclusive).toHours())
    }
}
