package com.example.money.domain.util

import com.example.money.domain.model.business.BusinessClient
import com.example.money.domain.model.business.BusinessDashboardSummary
import com.example.money.domain.model.business.BusinessInvoice
import com.example.money.domain.model.business.BusinessLedgerType
import com.example.money.domain.model.business.BusinessTransaction
import com.example.money.domain.model.business.BusinessTransactionType
import com.example.money.domain.model.business.CategoryAmountRow
import com.example.money.domain.model.business.ClientRevenueRow
import com.example.money.domain.model.business.InvoiceStatus
import java.util.Calendar

object BusinessFinanceEngine {

    fun monthRange(referenceMillis: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            timeInMillis = referenceMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = Calendar.getInstance().apply {
            timeInMillis = start.timeInMillis
            add(Calendar.MONTH, 1)
        }
        return start.timeInMillis to end.timeInMillis
    }

    fun computeDashboard(
        transactions: List<BusinessTransaction>,
        invoices: List<BusinessInvoice>,
        clients: List<BusinessClient>,
        monthStartMillis: Long,
        monthEndMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
    ): BusinessDashboardSummary {
        val monthTx = transactions.filter { tx ->
            tx.date >= monthStartMillis && tx.date < monthEndMillis &&
                tx.ledgerType != BusinessLedgerType.PERSONAL
        }
        val revenueCents = monthTx
            .filter { it.type == BusinessTransactionType.INCOME }
            .sumOf { it.amountCents }
        val expenseCents = monthTx
            .filter { it.type == BusinessTransactionType.EXPENSE }
            .sumOf { it.amountCents }

        val clientNameById = clients.associate { it.id to it.name }
        val revenueByClient = monthTx
            .filter { it.type == BusinessTransactionType.INCOME && it.clientId != null }
            .groupBy { it.clientId!! }
            .map { (clientId, rows) ->
                ClientRevenueRow(
                    clientId = clientId,
                    clientName = clientNameById[clientId] ?: "—",
                    revenueCents = rows.sumOf { it.amountCents },
                )
            }
            .sortedByDescending { it.revenueCents }
            .take(5)

        val expenseByCategory = monthTx
            .filter { it.type == BusinessTransactionType.EXPENSE }
            .groupBy { it.category }
            .map { (category, rows) ->
                CategoryAmountRow(
                    category = category,
                    amountCents = rows.sumOf { it.amountCents },
                )
            }
            .sortedByDescending { it.amountCents }
            .take(5)

        val openInvoices = invoices.filter {
            it.status == InvoiceStatus.SENT || it.status == InvoiceStatus.OVERDUE
        }
        val overdueReceivables = openInvoices
            .filter { it.dueDate < nowMillis }
            .sumOf { it.amountCents }
        val expectedReceivables = openInvoices.sumOf { it.amountCents }

        return BusinessDashboardSummary(
            monthRevenueCents = revenueCents,
            monthExpenseCents = expenseCents,
            monthNetProfitCents = revenueCents - expenseCents,
            overdueReceivablesCents = overdueReceivables,
            expectedReceivablesCents = expectedReceivables,
            topClients = revenueByClient,
            topExpenseCategories = expenseByCategory,
        )
    }

    fun resolveInvoiceStatus(invoice: BusinessInvoice, nowMillis: Long): InvoiceStatus {
        if (invoice.status == InvoiceStatus.PAID || invoice.status == InvoiceStatus.DRAFT) {
            return invoice.status
        }
        return if (invoice.dueDate < nowMillis) InvoiceStatus.OVERDUE else invoice.status
    }
}
