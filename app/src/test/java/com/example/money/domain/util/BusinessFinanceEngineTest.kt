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
            projects = emptyList(),
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

    @Test
    fun periodRangeForPreset_todayIsOneDay() {
        val cal = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.JULY, 14, 15, 30, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val (start, end) = BusinessFinanceEngine.periodRangeForPreset(
            com.example.money.domain.model.business.BusinessFilterPreset.TODAY,
            cal.timeInMillis,
        )
        assertEquals(end - start, java.util.concurrent.TimeUnit.DAYS.toMillis(1))
    }

    @Test
    fun buildCategoryDistribution_groupsIncomeAndExpense() {
        val monthStart = 1_000L
        val monthEnd = 9_000L
        val transactions = listOf(
            BusinessTransaction(
                id = 1,
                type = BusinessTransactionType.INCOME,
                amountCents = 30_000,
                category = "Services",
                date = 2_000,
            ),
            BusinessTransaction(
                id = 2,
                type = BusinessTransactionType.EXPENSE,
                amountCents = 10_000,
                category = "Services",
                date = 3_000,
            ),
            BusinessTransaction(
                id = 3,
                type = BusinessTransactionType.EXPENSE,
                amountCents = 5_000,
                category = "Software",
                date = 4_000,
            ),
        )
        val summary = BusinessFinanceEngine.computeDashboard(
            transactions = transactions,
            invoices = emptyList(),
            clients = emptyList(),
            projects = emptyList(),
            monthStartMillis = monthStart,
            monthEndMillis = monthEnd,
        )
        val services = summary.categoryDistribution.find { it.category == "Services" }
        assertEquals(30_000, services?.incomeCents)
        assertEquals(10_000, services?.expenseCents)
        assertEquals(true, summary.categoryDistribution.isNotEmpty())
    }

    @Test
    fun computeDashboard_includesAttentionAlertsForOverdue() {
        val monthStart = 1_000L
        val monthEnd = 9_000_000L
        val now = 8_000_000L
        val summary = BusinessFinanceEngine.computeDashboard(
            transactions = emptyList(),
            invoices = listOf(
                BusinessInvoice(
                    id = 1,
                    clientId = 1,
                    issueDate = 1_000,
                    dueDate = 5_000,
                    amountCents = 10_000,
                    status = InvoiceStatus.SENT,
                ),
            ),
            clients = listOf(BusinessClient(id = 1, name = "Acme")),
            projects = emptyList(),
            monthStartMillis = monthStart,
            monthEndMillis = monthEnd,
            nowMillis = now,
        )
        assertEquals(true, summary.attentionAlerts.any {
            it.type == com.example.money.domain.model.business.BusinessAttentionType.OVERDUE_INVOICE
        })
    }
}
