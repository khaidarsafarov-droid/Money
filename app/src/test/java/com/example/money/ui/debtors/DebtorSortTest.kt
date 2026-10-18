package com.example.money.ui.debtors

import com.example.money.domain.model.Debtor
import com.example.money.domain.model.DebtStatus
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

    private fun debtor(
        id: Long,
        due: Long,
        paid: Boolean = false,
    ): Debtor = Debtor(
        id = id,
        name = "Person $id",
        targetAmountCents = 10_000,
        currentAmountCents = if (paid) 10_000 else 0,
        status = if (paid) DebtStatus.COMPLETED else DebtStatus.ACTIVE,
        dateGiven = due - 86_400_000L,
        dateDue = due,
    )
}
