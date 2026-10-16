package com.example.money.domain.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class BudgetPeriod(
    val startInclusive: Instant,
    val endExclusive: Instant,
) {
    val startMillis: Long
        get() = startInclusive.toEpochMilli()

    val endMillis: Long
        get() = endExclusive.toEpochMilli()

    fun contains(epochMillis: Long): Boolean =
        epochMillis >= startMillis && epochMillis < endMillis
}

object BudgetPeriods {

    fun monthContaining(
        instant: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): BudgetPeriod {
        val start = instant.atZone(zone).toLocalDate().withDayOfMonth(1).atStartOfDay(zone)
        return BudgetPeriod(
            startInclusive = start.toInstant(),
            endExclusive = start.plusMonths(1).toInstant(),
        )
    }

    fun weekContaining(
        instant: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
        firstDayOfWeek: DayOfWeek = WeekFieldsFirstDay.current(),
    ): BudgetPeriod {
        val startDate = instant.atZone(zone).toLocalDate().with(
            TemporalAdjusters.previousOrSame(firstDayOfWeek),
        )
        val start = startDate.atStartOfDay(zone)
        return BudgetPeriod(
            startInclusive = start.toInstant(),
            endExclusive = start.plusWeeks(1).toInstant(),
        )
    }
}

internal object WeekFieldsFirstDay {
    fun current(): DayOfWeek = java.time.temporal.WeekFields.of(Locale.getDefault()).firstDayOfWeek
}
