package com.example.money.data.backup

import com.example.money.domain.backup.ImportWarning
import com.example.money.domain.model.Debtor
import com.example.money.notification.DebtReminderScheduler
import com.example.money.settings.PersonalBranchSettings
import kotlinx.coroutines.CancellationException

fun interface PostRestoreReconciler {
    suspend fun reconcile(): List<ImportWarning>

    companion object {
        val NO_OP = PostRestoreReconciler { emptyList() }
    }
}

class DefaultPostRestoreReconciler(
    private val reconcileDebtReminders: suspend () -> Unit,
    private val reconcilePersonalDebtMessaging: suspend () -> Unit,
    private val reconcileAutopilot: suspend () -> Unit,
    private val reconcileWeeklyCheckIn: suspend () -> Unit,
    private val reconcileVoiceReports: suspend () -> Unit,
    private val refreshWidgetSnapshot: suspend () -> Unit,
    private val reconcileCalendar: suspend () -> Unit,
) : PostRestoreReconciler {

    override suspend fun reconcile(): List<ImportWarning> = buildList {
        runStep("debt_reminders", reconcileDebtReminders)?.let(::add)
        runStep("personal_debt_messaging", reconcilePersonalDebtMessaging)?.let(::add)
        runStep("autopilot", reconcileAutopilot)?.let(::add)
        runStep("weekly_check_in", reconcileWeeklyCheckIn)?.let(::add)
        runStep("voice_reports", reconcileVoiceReports)?.let(::add)
        runStep("widget", refreshWidgetSnapshot)?.let(::add)
        runStep("calendar", reconcileCalendar)?.let(::add)
    }

    private suspend fun runStep(
        subsystem: String,
        action: suspend () -> Unit,
    ): ImportWarning? = try {
        action()
        null
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        ImportWarning(
            subsystem = subsystem,
            message = error.message ?: error::class.java.simpleName,
        )
    }
}

class DebtReminderRestoreReconciler(
    private val scheduler: DebtReminderScheduler,
    private val loadSettings: suspend () -> PersonalBranchSettings,
    private val loadActiveDebtors: suspend () -> List<Debtor>,
) {
    suspend fun reconcile() {
        scheduler.cancelAll()
        val settings = loadSettings()
        loadActiveDebtors().forEach { debtor ->
            if (!settings.notificationsEnabledFor(debtor.mode)) return@forEach
            scheduler.schedule(
                debtor,
                reminderHour = settings.reminderHour,
                reminderMinute = settings.reminderMinute,
                preserveSnooze = false,
            )
            scheduler.scheduleAdvanceReminders(debtor, settings)
        }
    }
}
