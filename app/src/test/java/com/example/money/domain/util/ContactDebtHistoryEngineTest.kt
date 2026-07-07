package com.example.money.domain.util

import com.example.money.domain.model.DebtStatus
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactDebtHistoryEngineTest {
    private fun sampleDebtor(
        id: Long,
        lookupKey: String?,
        name: String,
        target: Long,
        current: Long = 0,
        isPaid: Boolean = false,
    ) = Debtor(
        id = id,
        name = name,
        targetAmountCents = target,
        currentAmountCents = current,
        status = if (isPaid) DebtStatus.COMPLETED else DebtStatus.ACTIVE,
        debtType = DebtType.CREDITOR,
        dateGiven = 1_000L,
        dateDue = 2_000L,
        contactLookupKey = lookupKey,
    )

    @Test
    fun aggregateGroupsDebtsByLookupKey() {
        val debts = listOf(
            sampleDebtor(1, "abc", "Anna", 10_000),
            sampleDebtor(2, "abc", "Anna", 5_000, current = 5_000, isPaid = true),
            sampleDebtor(3, "xyz", "Bob", 1_000),
        )

        val history = ContactDebtHistoryEngine.aggregate(debts, "abc", "Anna")

        assertEquals(2, history.debts.size)
        assertEquals(1, history.activeDebtCount)
        assertEquals(1, history.closedDebtCount)
        assertEquals(10_000, history.activeRemainingCents)
    }

    @Test
    fun countExistingDebtsReturnsMatchesOnly() {
        val debts = listOf(
            sampleDebtor(1, "abc", "Anna", 1_000),
            sampleDebtor(2, null, "Manual", 2_000),
        )

        assertEquals(1, ContactDebtHistoryEngine.countExistingDebts(debts, "abc"))
    }
}
