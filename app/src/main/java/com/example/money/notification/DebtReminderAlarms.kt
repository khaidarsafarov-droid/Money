package com.example.money.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log

internal object DebtReminderAlarms {
    const val ACTION = "com.example.money.action.DEBT_REMINDER_ALARM"
    const val EXTRA_DEBTOR_ID = "extra_debtor_id"
    const val EXTRA_KIND = "extra_kind"
    const val EXTRA_DAYS_BEFORE = "extra_days_before"
    const val EXTRA_SNOOZE = "extra_snooze"
    const val KIND_DUE = "due"
    const val KIND_ADVANCE = "advance"

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    fun scheduleDue(context: Context, debtorId: Long, triggerAt: Long, snooze: Boolean): Boolean =
        schedule(
            context = context,
            debtorId = debtorId,
            kind = KIND_DUE,
            daysBefore = 0,
            triggerAt = triggerAt,
            snooze = snooze,
        )

    fun scheduleAdvance(context: Context, debtorId: Long, daysBefore: Int, triggerAt: Long): Boolean =
        schedule(
            context = context,
            debtorId = debtorId,
            kind = KIND_ADVANCE,
            daysBefore = daysBefore,
            triggerAt = triggerAt,
            snooze = false,
        )

    fun cancelDue(context: Context, debtorId: Long) {
        cancel(context, debtorId, KIND_DUE, daysBefore = 0)
    }

    fun cancelAdvance(context: Context, debtorId: Long, daysBefore: Int) {
        cancel(context, debtorId, KIND_ADVANCE, daysBefore)
    }

    private fun schedule(
        context: Context,
        debtorId: Long,
        kind: String,
        daysBefore: Int,
        triggerAt: Long,
        snooze: Boolean,
    ): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        val pending = pendingIntent(context, debtorId, kind, daysBefore, snooze)
        if (!canScheduleExact(context)) {
            alarmManager.cancel(pending)
            return false
        }
        return try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            true
        } catch (exception: SecurityException) {
            Log.w(TAG, "Exact alarm not allowed for debtor $debtorId", exception)
            alarmManager.cancel(pending)
            false
        }
    }

    private fun cancel(context: Context, debtorId: Long, kind: String, daysBefore: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(pendingIntent(context, debtorId, kind, daysBefore, snooze = false))
    }

    private fun pendingIntent(
        context: Context,
        debtorId: Long,
        kind: String,
        daysBefore: Int,
        snooze: Boolean,
    ): PendingIntent {
        val intent = Intent(context, DebtReminderAlarmReceiver::class.java).apply {
            action = ACTION
            data = Uri.parse("money://reminder/$kind/$debtorId/$daysBefore")
            putExtra(EXTRA_DEBTOR_ID, debtorId)
            putExtra(EXTRA_KIND, kind)
            putExtra(EXTRA_DAYS_BEFORE, daysBefore)
            putExtra(EXTRA_SNOOZE, snooze)
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private const val TAG = "DebtReminderAlarms"
}
