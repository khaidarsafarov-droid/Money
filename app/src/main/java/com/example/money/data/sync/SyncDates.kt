package com.example.money.data.sync

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

object SyncDates {
    fun formatDate(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()

    fun parseDate(iso: String): Long =
        LocalDate.parse(iso).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun formatDateTime(millis: Long): String =
        Instant.ofEpochMilli(millis).toString()

    fun parseDateTime(iso: String): Long =
        Instant.parse(iso).toEpochMilli()
}
