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
