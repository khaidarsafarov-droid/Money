package com.example.money.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.LoanCategory
import com.example.money.settings.CalendarSettings
import com.example.money.ui.util.Formatters
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CalendarSyncResult(
    val eventId: String?,
    val failed: Boolean,
)

class DebtCalendarSyncService(
    private val context: Context,
) {
    fun hasPermissions(): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    suspend fun upsertDebtEvent(
        debtor: Debtor,
        settings: CalendarSettings,
        displayName: String = "",
        markClosed: Boolean = false,
        force: Boolean = false,
    ): CalendarSyncResult = withContext(Dispatchers.IO) {
        if ((!force && !settings.autoSyncEnabled) || !hasPermissions()) {
            return@withContext CalendarSyncResult(debtor.calendarEventId, failed = !hasPermissions())
        }

        try {
            val calendarId = resolveCalendarId(settings) ?: return@withContext CalendarSyncResult(
                debtor.calendarEventId,
                failed = true,
            )
            val startMillis = dueDateAtNineAm(debtor.dateDue)
            val endMillis = startMillis + 60 * 60 * 1000
            val title = DebtCalendarText.buildTitle(debtor, markClosed)
            val description = DebtCalendarText.buildDescription(debtor, displayName)
            val deepLink = debtDeepLink(debtor.id)

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description)
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.ALL_DAY, 0)
                put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
                put(CalendarContract.Events.CUSTOM_APP_URI, deepLink)
            }

            val resolver = context.contentResolver
            val storedEventId = debtor.calendarEventId?.toLongOrNull()
            val ownedStoredEvent = storedEventId?.let { eventBelongsToDebtor(it, debtor.id) } == true

            val savedId = when {
                ownedStoredEvent -> {
                    val updated = resolver.update(
                        ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, storedEventId!!),
                        values,
                        null,
                        null,
                    )
                    if (updated > 0) {
                        replaceReminders(storedEventId, settings)
                        storedEventId
                    } else {
                        insertDebtEvent(values, settings)
                    }
                }
                else -> insertDebtEvent(values, settings)
            }

            CalendarSyncResult(savedId?.toString(), failed = savedId == null)
        } catch (_: SecurityException) {
            CalendarSyncResult(debtor.calendarEventId, failed = true)
        } catch (_: Exception) {
            CalendarSyncResult(debtor.calendarEventId, failed = true)
        }
    }

    suspend fun deleteDebtEvent(eventId: String?, debtorId: Long? = null): Boolean = withContext(Dispatchers.IO) {
        if (!hasPermissions() || eventId.isNullOrBlank()) return@withContext false
        return@withContext try {
            val id = eventId.toLongOrNull() ?: return@withContext false
            if (debtorId != null && !eventBelongsToDebtor(id, debtorId)) {
                return@withContext false
            }
            val rows = context.contentResolver.delete(
                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id),
                null,
                null,
            )
            rows > 0
        } catch (_: Exception) {
            false
        }
    }

    suspend fun ensureDebtsCalendar(settings: CalendarSettings): Long? = withContext(Dispatchers.IO) {
        if (!hasPermissions()) return@withContext null
        settings.debtsCalendarId?.let { return@withContext it }
        resolveCalendarId(settings)
    }

    private fun insertDebtEvent(values: ContentValues, settings: CalendarSettings): Long? {
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        val newId = uri?.lastPathSegment?.toLongOrNull()
        newId?.let { replaceReminders(it, settings) }
        return newId
    }

    private fun eventBelongsToDebtor(eventId: Long, debtorId: Long): Boolean {
        val resolver = context.contentResolver
        resolver.query(
            ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId),
            arrayOf(CalendarContract.Events.CUSTOM_APP_URI),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return false
            val deepLink = cursor.getString(0)
            return deepLink == debtDeepLink(debtorId)
        }
        return false
    }

    private fun debtDeepLink(debtorId: Long): String = "money://debt/$debtorId"

    private fun calendarExists(calendarId: Long): Boolean {
        val resolver = context.contentResolver
        resolver.query(
            ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calendarId),
            arrayOf(CalendarContract.Calendars._ID),
            null,
            null,
            null,
        )?.use { cursor ->
            return cursor.moveToFirst()
        }
        return false
    }

    private fun resolveCalendarId(settings: CalendarSettings): Long? {
        settings.debtsCalendarId?.let { storedId ->
            if (calendarExists(storedId)) return storedId
        }
        val resolver = context.contentResolver
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
        )
        resolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE} = 1",
            null,
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accessIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
            var writablePrimaryId: Long? = null
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex) ?: ""
                val access = cursor.getInt(accessIndex)
                if (settings.useDebtsCalendar && name == DEBTS_CALENDAR_NAME && isWritableAccess(access)) {
                    return id
                }
                if (writablePrimaryId == null && isWritableAccess(access)) {
                    writablePrimaryId = id
                }
            }
            if (settings.useDebtsCalendar) {
                return createDebtsCalendar() ?: writablePrimaryId
            }
            return writablePrimaryId
        }
        return null
    }

    private fun isWritableAccess(accessLevel: Int): Boolean =
        accessLevel >= CalendarContract.Calendars.CALENDAR_ACCESS_CONTRIBUTOR

    private fun createDebtsCalendar(): Long? {
        val resolver = context.contentResolver
        val account = resolveWritableAccount() ?: return null
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, account.first)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, account.second)
            put(CalendarContract.Calendars.NAME, DEBTS_CALENDAR_NAME)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, DEBTS_CALENDAR_NAME)
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFF6B6BFF.toInt())
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CALENDAR_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, account.first)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
        }
        val uri = resolver.insert(
            CalendarContract.Calendars.CONTENT_URI.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account.first)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, account.second)
                .build(),
            values,
        ) ?: run {
            resolver.insert(CalendarContract.Calendars.CONTENT_URI, values)
        }
        return uri?.lastPathSegment?.toLongOrNull()
    }

    private fun resolveWritableAccount(): Pair<String, String>? {
        val resolver = context.contentResolver
        resolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            ),
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?",
            arrayOf(CalendarContract.Calendars.CALENDAR_ACCESS_CONTRIBUTOR.toString()),
            null,
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            val typeIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
            val accessIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
            var best: Pair<String, String>? = null
            var bestAccess = -1
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex) ?: continue
                val type = cursor.getString(typeIndex) ?: continue
                val access = cursor.getInt(accessIndex)
                if (access > bestAccess) {
                    bestAccess = access
                    best = name to type
                }
            }
            return best
        }
        return null
    }

    private fun replaceReminders(eventId: Long, settings: CalendarSettings) {
        val resolver = context.contentResolver
        resolver.delete(
            CalendarContract.Reminders.CONTENT_URI,
            "${CalendarContract.Reminders.EVENT_ID} = ?",
            arrayOf(eventId.toString()),
        )
        reminderOffsets(settings).forEach { minutes ->
            val values = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                put(CalendarContract.Reminders.MINUTES, minutes)
            }
            resolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
        }
    }

    private fun reminderOffsets(settings: CalendarSettings): List<Int> = buildList {
        if (settings.remindThreeDaysBefore) add(3 * 24 * 60)
        if (settings.remindOneDayBefore) add(24 * 60)
        if (settings.remindOnDueDay) add(0)
    }

    private fun dueDateAtNineAm(dueMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dueMillis
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    companion object {
        const val DEBTS_CALENDAR_NAME = "Долги"

        fun debtDeepLinkIntent(debtorId: Long): Intent =
            Intent(Intent.ACTION_VIEW, Uri.parse("money://debt/$debtorId"))
    }
}

object DebtCalendarText {
    fun buildTitle(debtor: Debtor, markClosed: Boolean = false): String {
        val prefix = if (markClosed || debtor.isPaid) "[ЗАКРЫТ] " else ""
        val amount = Formatters.currencyFromCents(debtor.remainingAmountCents, debtor.currency)
        return when (debtor.debtType) {
            DebtType.CREDITOR -> "$prefix${debtor.name} должен(на) мне $amount"
            DebtType.DEBTOR -> "$prefixЯ должен(на) ${debtor.name} $amount"
        }
    }

    fun buildDescription(debtor: Debtor, displayName: String): String = buildString {
        appendLine("Сумма: ${Formatters.currencyFromCents(debtor.remainingAmountCents, debtor.currency)}")
        appendLine("Изначально: ${Formatters.currencyFromCents(debtor.targetAmountCents, debtor.currency)}")
        appendLine("Дата выдачи: ${Formatters.shortDate(debtor.dateGiven)}")
        if (debtor.interestRateBps > 0) {
            appendLine("Процентная ставка: ${"%.2f".format(debtor.interestRateBps / 100.0)}%")
        }
        if (debtor.debtType == DebtType.DEBTOR) {
            appendLine("Категория: ${loanCategoryLabel(debtor.loanCategory)}")
        }
        if (displayName.isNotBlank()) {
            append("Пользователь: $displayName")
        }
    }

    private fun loanCategoryLabel(category: LoanCategory): String = when (category) {
        LoanCategory.CREDIT_CARD -> "Кредитная карта"
        LoanCategory.PERSONAL_LOAN -> "Личный займ"
        LoanCategory.MORTGAGE -> "Ипотека"
        LoanCategory.STUDENT_LOAN -> "Образование"
        LoanCategory.MEDICAL -> "Медицина"
        LoanCategory.OTHER -> "Другое"
    }
}
