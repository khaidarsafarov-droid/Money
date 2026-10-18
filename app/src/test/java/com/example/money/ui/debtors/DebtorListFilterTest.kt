package com.example.money.ui.debtors

import com.example.money.domain.model.Debtor
import com.example.money.domain.model.DebtStatus
import com.example.money.domain.model.DebtType
import com.example.money.domain.util.DebtDueDateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtorListFilterTest {

    private val now = 1_700_000_000_000L

    @Test
    fun activeFilter_includesOverdueUnpaid() {
        val overdue = debtor(id = 1, due = now - 86_400_000L)
        val future = debtor(id = 2, due = now + 86_400_000L)

        val result = DebtorListFilter.apply(
            debtors = listOf(overdue, future),
            tab = DebtTab.OwesMe,
            filter = DebtorFilter.ACTIVE,
            searchQuery = "",
            now = now,
        )

        assertEquals(listOf(1L, 2L), result.map { it.id })
    }

    @Test
    fun overdueFilter_ordersOldestFirstAndSkipsUndated() {
        val oldest = debtor(id = 1, due = now - 172_800_000L)
        val newer = debtor(id = 2, due = now - 86_400_000L)
        val undated = debtor(id = 3, due = 0L)
        val future = debtor(id = 4, due = now + 86_400_000L)

        val result = DebtorListFilter.apply(
            debtors = listOf(future, undated, newer, oldest),
            tab = DebtTab.OwesMe,
            filter = DebtorFilter.OVERDUE,
            searchQuery = "",
            now = now,
        )

        assertEquals(listOf(1L, 2L), result.map { it.id })
    }

    @Test
    fun search_matchesEmail() {
        val debtor = debtor(id = 1, due = now + 86_400_000L).copy(email = "alex@mail.com")

        val result = DebtorListFilter.apply(
            debtors = listOf(debtor),
            tab = DebtTab.OwesMe,
            filter = DebtorFilter.ALL,
            searchQuery = "alex@mail",
            now = now,
        )

        assertEquals(1, result.size)
    }

    @Test
    fun dueDateUtils_overdueAfterEndOfDueDay() {
        val due = now
        assertFalse(DebtDueDateUtils.isOverdue(due, now))
        assertTrue(DebtDueDateUtils.isOverdue(due, DebtDueDateUtils.endOfDayMillis(due) + 1L))
    }

    @Test
    fun overdueBefore_keepsDueDayOutOfOverdueFilter() {
        val dueTodayNoon = DebtDueDateUtils.startOfDayMillis(now) + 12L * 60L * 60L * 1000L
        val overdueBefore = DebtDueDateUtils.overdueBeforeMillis(now)
        assertTrue(dueTodayNoon >= overdueBefore)
        assertFalse(DebtDueDateUtils.isOverdue(dueTodayNoon, now))
    }

    @Test
    fun overdueBefore_marksPreviousDayAsOverdue() {
        val dueYesterday = DebtDueDateUtils.startOfDayMillis(now) - 12L * 60L * 60L * 1000L
        val overdueBefore = DebtDueDateUtils.overdueBeforeMillis(now)
        assertTrue(dueYesterday < overdueBefore)
        assertTrue(DebtDueDateUtils.isOverdue(dueYesterday, now))
    }

    private fun debtor(id: Long, due: Long): Debtor = Debtor(
        id = id,
        name = "Person $id",
        targetAmountCents = 10_000,
        currentAmountCents = 0,
        status = DebtStatus.ACTIVE,
        debtType = DebtType.CREDITOR,
        dateGiven = due - 86_400_000L,
        dateDue = due,
    )
}
