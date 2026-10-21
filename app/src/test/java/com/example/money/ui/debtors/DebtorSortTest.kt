package com.example.money.ui.debtors

import com.example.money.domain.model.Debtor
import com.example.money.domain.model.DebtStatus
import com.example.money.domain.util.DebtDueDateUtils
import com.example.money.settings.AppMode
import org.junit.Assert.assertEquals
import org.junit.Test

class DebtorSortTest {

    @Test
    fun sort_placesOldestOverdueFirstThenNearestActiveThenUndatedThenPaid() {
        val now = 1_700_000_000_000L
        val debtors = listOf(
            debtor(id = 1, due = now + 172_800_000L),
            debtor(id = 2, due = now - 86_400_000L),
            debtor(id = 3, due = now + 86_400_000L),
            debtor(id = 4, due = now - 172_800_000L),
            debtor(id = 5, due = 0L),
            debtor(id = 6, due = now - 86_400_000L, paid = true),
        )

        val sorted = DebtorSort.sort(debtors, now = now)

        assertEquals(listOf(4L, 2L, 3L, 1L, 5L, 6L), sorted.map { it.id })
    }

    @Test
    fun workMode_putsDueTodayBeforeOverdueThenNearestThenUndatedThenPaid() {
        val now = DebtDueDateUtils.startOfDayMillis(1_700_000_000_000L) + 15 * 60 * 60 * 1000L
        val today = DebtDueDateUtils.startOfDayMillis(now) + 12 * 60 * 60 * 1000L
        val debtors = listOf(
            debtor(id = 1, due = today + 2 * 86_400_000L, mode = AppMode.PROFESSIONAL),
            debtor(id = 2, due = today - 86_400_000L, mode = AppMode.PROFESSIONAL),
            debtor(id = 3, due = today, mode = AppMode.PROFESSIONAL),
            debtor(id = 4, due = today - 2 * 86_400_000L, mode = AppMode.PROFESSIONAL),
            debtor(id = 5, due = 0L, mode = AppMode.PROFESSIONAL),
            debtor(id = 6, due = today - 86_400_000L, paid = true, mode = AppMode.PROFESSIONAL),
            debtor(id = 7, due = today + 86_400_000L, mode = AppMode.PROFESSIONAL),
        )

        val sorted = DebtorSort.sort(debtors, now = now)

        assertEquals(listOf(3L, 4L, 2L, 7L, 1L, 5L, 6L), sorted.map { it.id })
    }

    private fun debtor(
        id: Long,
        due: Long,
        paid: Boolean = false,
        mode: com.example.money.settings.AppMode = com.example.money.settings.AppMode.PERSONAL,
    ): Debtor = Debtor(
        id = id,
        name = "Person $id",
        targetAmountCents = 10_000,
        currentAmountCents = if (paid) 10_000 else 0,
        status = if (paid) DebtStatus.COMPLETED else DebtStatus.ACTIVE,
        dateGiven = due - 86_400_000L,
        dateDue = due,
        mode = mode,
    )
}
