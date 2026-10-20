package com.example.money.notification

import android.content.Context

/**
 * Snooze fire times and last-shown days live outside DataStore so app start
 * can keep a pending snooze without a sync-schema change.
 */
class DebtReminderState(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun snoozeFireAt(debtorId: Long): Long? =
        prefs.getLong(snoozeKey(debtorId), ABSENT).takeIf { it >= 0L }

    fun setSnoozeFireAt(debtorId: Long, triggerAt: Long) {
        prefs.edit().putLong(snoozeKey(debtorId), triggerAt).commit()
    }

    fun clearSnooze(debtorId: Long) {
        prefs.edit().remove(snoozeKey(debtorId)).commit()
    }

    fun lastShownDayStart(debtorId: Long): Long? =
        prefs.getLong(shownKey(debtorId), ABSENT).takeIf { it >= 0L }

    fun setLastShownDayStart(debtorId: Long, dayStart: Long) {
        prefs.edit().putLong(shownKey(debtorId), dayStart).commit()
    }

    fun track(debtorId: Long) {
        prefs.edit().putBoolean(trackKey(debtorId), true).commit()
    }

    fun trackedIds(): Set<Long> = prefs.all.keys.mapNotNull { key ->
        if (!key.startsWith(TRACK_PREFIX)) return@mapNotNull null
        key.removePrefix(TRACK_PREFIX).toLongOrNull()
    }.toSet()

    fun clear(debtorId: Long) {
        prefs.edit()
            .remove(snoozeKey(debtorId))
            .remove(shownKey(debtorId))
            .remove(trackKey(debtorId))
            .commit()
    }

    fun clearAll() {
        prefs.edit().clear().commit()
    }

    private fun snoozeKey(debtorId: Long) = "snooze_$debtorId"
    private fun shownKey(debtorId: Long) = "shown_$debtorId"
    private fun trackKey(debtorId: Long) = "$TRACK_PREFIX$debtorId"

    private companion object {
        const val PREFS = "debt_reminder_state"
        const val TRACK_PREFIX = "track_"
        const val ABSENT = -1L
    }
}
