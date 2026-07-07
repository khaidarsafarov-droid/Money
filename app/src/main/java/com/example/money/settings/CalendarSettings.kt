package com.example.money.settings

data class CalendarSettings(
    val autoSyncEnabled: Boolean = true,
    val remindOneDayBefore: Boolean = true,
    val remindThreeDaysBefore: Boolean = false,
    val remindOnDueDay: Boolean = true,
    val useDebtsCalendar: Boolean = true,
    val debtsCalendarId: Long? = null,
    val permissionRequested: Boolean = false,
)
