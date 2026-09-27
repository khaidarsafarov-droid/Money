package com.example.money.notification

import com.example.money.domain.model.Debtor
import com.example.money.domain.model.DebtStatus
import com.example.money.settings.PersonalBranchSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class DebtReminderCoordinatorTest {

    @Test
    fun sync_skipsUnchangedDebtor() {
        val scheduler = RecordingScheduler()
        val settings = PersonalBranchSettings(debtNotificationsEnabled = true)
        val debtor = sampleDebtor(id = 1L)
        val signature = DebtReminderCoordinator.ReminderSignature.from(debtor, settings)

        val first = DebtReminderCoordinator.syncAdvanceReminders(
            scheduler = scheduler,
            settings = settings,
            previous = emptyMap(),
            debtors = listOf(debtor),
        )
        val second = DebtReminderCoordinator.syncAdvanceReminders(
            scheduler = scheduler,
            settings = settings,
            previous = first,
            debtors = listOf(debtor),
        )

        assertEquals(1, scheduler.scheduleCount)
        assertEquals(1, scheduler.dueScheduleCount)
        assertEquals(signature, second[1L])
    }

    @Test
    fun sync_cancelsPaidDebtor() {
        val scheduler = RecordingScheduler()
        val settings = PersonalBranchSettings()
        val active = sampleDebtor(id = 2L)
        val previous = DebtReminderCoordinator.syncAdvanceReminders(
            scheduler = scheduler,
            settings = settings,
            previous = emptyMap(),
            debtors = listOf(active),
        )

        DebtReminderCoordinator.syncAdvanceReminders(
            scheduler = scheduler,
            settings = settings,
            previous = previous,
            debtors = emptyList(),
        )

        assertEquals(listOf(2L), scheduler.cancelledIds)
    }

    @Test
    fun sync_disablingNotificationsCancelsDueAndAdvance() {
        val scheduler = RecordingScheduler()
        val enabled = PersonalBranchSettings(debtNotificationsEnabled = true)
        val disabled = PersonalBranchSettings(debtNotificationsEnabled = false)
        val debtor = sampleDebtor(id = 3L)
        val previous = DebtReminderCoordinator.syncAdvanceReminders(
            scheduler = scheduler,
            settings = enabled,
            previous = emptyMap(),
            debtors = listOf(debtor),
        )

        DebtReminderCoordinator.syncAdvanceReminders(
            scheduler = scheduler,
            settings = disabled,
            previous = previous,
            debtors = listOf(debtor),
        )

        assertEquals(listOf(3L), scheduler.cancelledIds)
    }

    private fun sampleDebtor(id: Long): Debtor = Debtor(
        id = id,
        name = "Test",
        targetAmountCents = 10_000,
        currentAmountCents = 0,
        status = DebtStatus.ACTIVE,
        dateGiven = 1_700_000_000_000L,
        dateDue = 1_700_100_000_000L,
    )

    private class RecordingScheduler : DebtReminderScheduler {
        var scheduleCount = 0
        var dueScheduleCount = 0
        val cancelledIds = mutableListOf<Long>()

        override fun schedule(debtor: Debtor) {
            dueScheduleCount += 1
        }

        override fun showNow(debtor: Debtor) = false
        override fun snooze(debtor: Debtor, delayMillis: Long) = Unit
        override fun cancel(debtorId: Long) {
            cancelledIds += debtorId
        }

        override suspend fun cancelAll() = Unit

        override fun scheduleDailyFollowUp(debtor: Debtor) = Unit
        override fun scheduleAdvanceReminders(debtor: Debtor, settings: PersonalBranchSettings) {
            scheduleCount += 1
        }

        override fun cancelAdvanceReminders(debtorId: Long) = Unit
    }
}
