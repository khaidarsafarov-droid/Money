package com.example.money.data.sync

import com.example.money.data.local.entity.DebtPaymentEntity
import com.example.money.domain.sync.FinanceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceSnapshotCodecTest {

    @Test
    fun decode_migratesSchemaV2RecordsToCurrentV3Contract() {
        val decoded = FinanceSnapshotCodec.decode(
            """
            {
              "schemaVersion": 2,
              "deviceId": "web-device",
              "updatedAt": 100,
              "goals": [],
              "debts": [{
                "id": "debt-1",
                "name": "Debt",
                "targetAmountCents": 1000,
                "currentAmountCents": 0,
                "currency": "USD",
                "status": "ACTIVE",
                "dateGiven": "2026-01-01",
                "syncUpdatedAt": 100
              }],
              "debtPayments": [{
                "id": "payment-1",
                "debtorId": "debt-1",
                "amountCents": 100,
                "paidAt": "2026-01-02T00:00:00Z",
                "syncUpdatedAt": 100
              }],
              "transactions": []
            }
            """.trimIndent(),
        )

        assertEquals(FinanceSnapshot.CURRENT_SCHEMA_VERSION, decoded.schemaVersion)
        assertEquals("CREDITOR", decoded.debts.single().debtType)
        assertFalse(decoded.debts.single().affectsBalance)
        assertEquals("", decoded.debts.single().note)
        assertTrue(decoded.debtPayments.single().affectsBalance)
        assertNull(decoded.debtPayments.single().currency)
    }

    @Test
    fun debtPaymentCurrencyRoundTrips() {
        val encoded = FinanceSnapshotCodec.encodeDebtPayment(
            DebtPaymentEntity(
                syncId = "payment-1",
                debtorId = 1L,
                amountCents = 100L,
                currency = "TJS",
                paidAt = 1_700_000_000_000L,
            ),
            debtorSyncId = "debt-1",
        )

        assertEquals("TJS", FinanceSnapshotCodec.decodeDebtPayment(encoded).currency)
    }
}
