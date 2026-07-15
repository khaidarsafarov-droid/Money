package com.example.money.domain.util.ai

import com.example.money.domain.model.business.BusinessInvoice
import com.example.money.domain.model.business.InvoiceRiskLevel
import com.example.money.domain.model.business.InvoiceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiInvoiceRiskServiceTest {

    @Test
    fun assess_marksOverdueAsHighRisk() {
        val invoice = BusinessInvoice(
            id = 1,
            clientId = 1,
            issueDate = 1_000,
            dueDate = 5_000,
            amountCents = 10_000,
            status = InvoiceStatus.OVERDUE,
        )
        val nowMillis = 5_000 + java.util.concurrent.TimeUnit.DAYS.toMillis(15)
        val result = AiInvoiceRiskService.assess(invoice, clientOverdueCount = 0, nowMillis = nowMillis)
        assertEquals(InvoiceRiskLevel.HIGH, result.riskLevel)
        assertTrue(result.probabilityPercent >= 60)
    }
}
