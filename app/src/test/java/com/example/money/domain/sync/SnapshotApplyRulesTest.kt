package com.example.money.domain.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotApplyRulesTest {

    @Test
    fun linkableDebtPayments_dropsPaymentsWhoseDebtorIsMissing() {
        val payments = listOf(
            payment(id = "pay-kept", debtorId = "debt-1"),
            payment(id = "pay-orphan", debtorId = "debt-missing"),
        )

        val linkable = linkableDebtPayments(payments, setOf("debt-1"))

        assertEquals(listOf("pay-kept"), linkable.map { it.id })
    }

    @Test
    fun snapshotTransactionBucket_ignoresUnknownKinds() {
        assertEquals(SnapshotTransactionBucket.EXPENSE, snapshotTransactionBucket("EXPENSE"))
        assertEquals(SnapshotTransactionBucket.TRANSFER, snapshotTransactionBucket("DEPOSIT"))
        assertEquals(SnapshotTransactionBucket.TRANSFER, snapshotTransactionBucket("GOAL_TRANSFER"))
        assertEquals(SnapshotTransactionBucket.IGNORED, snapshotTransactionBucket("REFUND"))
    }

    private fun payment(id: String, debtorId: String) = SnapshotDebtPayment(
        id = id,
        debtorId = debtorId,
        amountCents = 100,
        paidAt = "2026-01-01T00:00:00Z",
        syncUpdatedAt = 1L,
    )
}
