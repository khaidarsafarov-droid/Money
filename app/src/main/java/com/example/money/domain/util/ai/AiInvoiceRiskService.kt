package com.example.money.domain.util.ai

import com.example.money.domain.model.business.BusinessClient
import com.example.money.domain.model.business.BusinessInvoice
import com.example.money.domain.model.business.InvoiceRiskAssessment
import com.example.money.domain.model.business.InvoiceRiskLevel
import com.example.money.domain.model.business.InvoiceStatus
import java.util.concurrent.TimeUnit

object AiInvoiceRiskService {

    fun assessAll(
        invoices: List<BusinessInvoice>,
        clients: List<BusinessClient>,
        nowMillis: Long = System.currentTimeMillis(),
    ): Map<Long, InvoiceRiskAssessment> {
        val overdueByClient = invoices
            .filter {
                (it.status == InvoiceStatus.SENT || it.status == InvoiceStatus.OVERDUE) &&
                    it.dueDate < nowMillis
            }
            .groupBy { it.clientId }
            .mapValues { it.value.size }

        return invoices
            .filter { it.status == InvoiceStatus.SENT || it.status == InvoiceStatus.OVERDUE }
            .associate { invoice ->
                invoice.id to assess(invoice, overdueByClient[invoice.clientId] ?: 0, nowMillis)
            }
    }

    fun assess(
        invoice: BusinessInvoice,
        clientOverdueCount: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): InvoiceRiskAssessment {
        val daysUntilDue = TimeUnit.MILLISECONDS.toDays(invoice.dueDate - nowMillis)
        val daysOverdue = if (invoice.dueDate < nowMillis) {
            TimeUnit.MILLISECONDS.toDays(nowMillis - invoice.dueDate)
        } else {
            0L
        }

        var probability = 10
        var level = InvoiceRiskLevel.LOW

        when {
            invoice.status == InvoiceStatus.OVERDUE || daysOverdue > 0 -> {
                probability = (60 + daysOverdue * 3).toInt().coerceAtMost(95)
                level = if (daysOverdue > 14) InvoiceRiskLevel.HIGH else InvoiceRiskLevel.MEDIUM
            }
            daysUntilDue <= 3 -> {
                probability = 45 + clientOverdueCount * 10
                level = InvoiceRiskLevel.MEDIUM
            }
            daysUntilDue <= 7 -> {
                probability = 30 + clientOverdueCount * 8
                level = if (clientOverdueCount > 0) InvoiceRiskLevel.MEDIUM else InvoiceRiskLevel.LOW
            }
            clientOverdueCount >= 2 -> {
                probability = 35
                level = InvoiceRiskLevel.MEDIUM
            }
            invoice.amountCents > 500_000 -> {
                probability = 25
            }
        }

        if (clientOverdueCount > 0 && level == InvoiceRiskLevel.LOW) {
            probability = (probability + clientOverdueCount * 5).coerceAtMost(40)
        }

        return InvoiceRiskAssessment(
            invoiceId = invoice.id,
            riskLevel = level,
            probabilityPercent = probability.coerceIn(5, 95),
        )
    }
}
