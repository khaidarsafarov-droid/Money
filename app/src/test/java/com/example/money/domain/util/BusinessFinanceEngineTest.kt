package com.example.money.domain.util

import com.example.money.domain.model.business.BusinessClient
import com.example.money.domain.model.business.BusinessInvoice
import com.example.money.domain.model.business.BusinessLedgerType
import com.example.money.domain.model.business.BusinessTransaction
import com.example.money.domain.model.business.BusinessTransactionType
import com.example.money.domain.model.business.InvoiceStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class BusinessFinanceEngineTest {

    @Test
    fun computeDashboard_sumsMonthlyIncomeAndExpenses() {
        val monthStart = 1_000L
        val monthEnd = 9_000L
        val transactions = listOf(
            BusinessTransaction(
                id = 1,
                type = BusinessTransactionType.INCOME,
                amountCents = 50_000,
                category = "Revenue",
                clientId = 1,
                date = 2_000,
            ),
            BusinessTransaction(
                id = 2,
                type = BusinessTransactionType.EXPENSE,
                amountCents = 20_000,
                category = "Software",
                ledgerType = BusinessLedgerType.OPERATING,
                date = 3_000,
            ),
            BusinessTransaction(
                id = 3,
                type = BusinessTransactionType.EXPENSE,
                amountCents = 5_000,
                category = "Personal",
                ledgerType = BusinessLedgerType.PERSONAL,
                date = 4_000,
            ),
        )

        val summary = BusinessFinanceEngine.computeDashboard(
            transactions = transactions,
            invoices = emptyList(),
            clients = listOf(BusinessClient(id = 1, name = "Acme")),
            monthStartMillis = monthStart,
            monthEndMillis = monthEnd,
        )

        assertEquals(50_000, summary.monthRevenueCents)
        assertEquals(20_000, summary.monthExpenseCents)
        assertEquals(30_000, summary.monthNetProfitCents)
        assertEquals(1, summary.topClients.size)
    }

    @Test
    fun resolveInvoiceStatus_marksSentAsOverdueAfterDueDate() {
        val invoice = BusinessInvoice(
            id = 1,
            clientId = 1,
            issueDate = 1_000,
            dueDate = 5_000,
            amountCents = 10_000,
            status = InvoiceStatus.SENT,
        )
        assertEquals(InvoiceStatus.OVERDUE, BusinessFinanceEngine.resolveInvoiceStatus(invoice, nowMillis = 6_000))
    }
}
