package com.example.money.data.backup

import com.example.money.domain.model.DebtStatus
import com.example.money.domain.model.Debtor
import com.example.money.notification.DebtReminderScheduler
import com.example.money.settings.PersonalBranchSettings
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostRestoreReconcilerTest {

    @Test
    fun reconcile_invokesEverySubsystemOnceInOrder() = runBlocking {
        val calls = mutableListOf<String>()
        val reconciler = DefaultPostRestoreReconciler(
            reconcileDebtReminders = { calls += "debt_reminders" },
            reconcilePersonalDebtMessaging = { calls += "personal_debt_messaging" },
            reconcileAutopilot = { calls += "autopilot" },
            reconcileWeeklyCheckIn = { calls += "weekly_check_in" },
            reconcileVoiceReports = { calls += "voice_reports" },
            refreshWidgetSnapshot = { calls += "widget" },
            reconcileCalendar = { calls += "calendar" },
        )

        val warnings = reconciler.reconcile()

        assertTrue(warnings.isEmpty())
        assertEquals(
            listOf(
                "debt_reminders",
                "personal_debt_messaging",
                "autopilot",
                "weekly_check_in",
                "voice_reports",
                "widget",
                "calendar",
            ),
            calls,
        )
    }

    @Test
    fun reconcile_reportsStepFailureAndContinuesRemainingSubsystems() = runBlocking {
        val calls = mutableListOf<String>()
        val reconciler = DefaultPostRestoreReconciler(
            reconcileDebtReminders = { calls += "debt_reminders" },
            reconcilePersonalDebtMessaging = {
                calls += "personal_debt_messaging"
                throw IOException("injected")
            },
            reconcileAutopilot = { calls += "autopilot" },
            reconcileWeeklyCheckIn = { calls += "weekly_check_in" },
            reconcileVoiceReports = { calls += "voice_reports" },
            refreshWidgetSnapshot = { calls += "widget" },
            reconcileCalendar = { calls += "calendar" },
        )

        val warnings = reconciler.reconcile()

        assertEquals(7, calls.size)
        assertEquals(1, warnings.size)
        assertEquals("personal_debt_messaging", warnings.single().subsystem)
    }
}

class DebtReminderRestoreReconcilerTest {

    @Test
    fun reconcile_cancelsAllBeforeSchedulingEveryActiveDebt() = runBlocking {
        val scheduler = RecordingDebtReminderScheduler()
        val settings = PersonalBranchSettings(
            debtNotificationsEnabled = true,
            debtAdvanceRemind1Day = true,
        )
        val debts = listOf(sampleDebtor(1L), sampleDebtor(2L))
        val reconciler = DebtReminderRestoreReconciler(
            scheduler = scheduler,
            loadSettings = { settings },
            loadActiveDebtors = { debts },
        )

        reconciler.reconcile()

        assertEquals(
            listOf("cancel_all", "schedule_1", "advance_1", "schedule_2", "advance_2"),
            scheduler.calls,
        )
    }

    @Test
    fun reconcile_whenNotificationsDisabled_onlyCancelsStaleState() = runBlocking {
        val scheduler = RecordingDebtReminderScheduler()
        val reconciler = DebtReminderRestoreReconciler(
            scheduler = scheduler,
            loadSettings = { PersonalBranchSettings(debtNotificationsEnabled = false) },
            loadActiveDebtors = { listOf(sampleDebtor(1L)) },
        )

        reconciler.reconcile()

        assertEquals(listOf("cancel_all"), scheduler.calls)
    }

    private fun sampleDebtor(id: Long) = Debtor(
        id = id,
        name = "Debt $id",
        targetAmountCents = 10_000,
        currentAmountCents = 1_000,
        status = DebtStatus.ACTIVE,
        dateGiven = 1_700_000_000_000L,
        dateDue = 1_800_000_000_000L,
    )

    private class RecordingDebtReminderScheduler : DebtReminderScheduler {
        val calls = mutableListOf<String>()

        override fun schedule(
            debtor: Debtor,
            reminderHour: Int,
            reminderMinute: Int,
            preserveSnooze: Boolean,
        ) {
            calls += "schedule_${debtor.id}"
        }

        override fun showNow(debtor: Debtor) = false

        override fun snooze(
            debtor: Debtor,
            delayMillis: Long,
            reminderHour: Int,
            reminderMinute: Int,
        ) = Unit

        override fun cancel(debtorId: Long) = Unit

        override suspend fun cancelAll() {
            calls += "cancel_all"
        }

        override fun scheduleDailyFollowUp(
            debtor: Debtor,
            reminderHour: Int,
            reminderMinute: Int,
        ) = Unit

        override fun scheduleAdvanceReminders(debtor: Debtor, settings: PersonalBranchSettings) {
            calls += "advance_${debtor.id}"
        }

        override fun cancelAdvanceReminders(debtorId: Long) = Unit
    }
}
