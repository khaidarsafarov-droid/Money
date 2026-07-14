package com.example.money.domain.util.ai

import com.example.money.domain.model.business.AiBusinessInsight
import com.example.money.domain.model.business.AiInsightSeverity
import com.example.money.domain.model.business.BusinessDashboardSummary
import com.example.money.domain.model.business.CategoryAmountRow
import com.example.money.domain.model.business.ClientProfitRow
import com.example.money.domain.model.business.OverdueInvoiceRow

object AiBusinessInsightsService {

    fun generate(
        summary: BusinessDashboardSummary,
        clientTable: List<ClientProfitRow>,
        overdueInvoices: List<OverdueInvoiceRow>,
        expenseCategories: List<CategoryAmountRow>,
    ): List<AiBusinessInsight> = buildList {
        val topClient = clientTable.firstOrNull()
        if (topClient != null && summary.monthNetProfitCents > 0) {
            val share = (topClient.profitCents.toDouble() / summary.monthNetProfitCents * 100).toInt()
            if (share >= 30) {
                add(
                    AiBusinessInsight(
                        message = "Client «${topClient.clientName}» contributes $share% of profit — consider expanding engagement.",
                        severity = AiInsightSeverity.INFO,
                    ),
                )
            }
        }

        val marketing = expenseCategories.find { it.category.equals("Marketing", ignoreCase = true) }
        if (marketing != null && summary.monthExpenseCents > 0) {
            val share = (marketing.amountCents.toDouble() / summary.monthExpenseCents * 100).toInt()
            if (share >= 15) {
                add(
                    AiBusinessInsight(
                        message = "Marketing is $share% of expenses — review channel ROI.",
                        severity = AiInsightSeverity.WARNING,
                    ),
                )
            }
        }

        overdueInvoices.firstOrNull()?.let { overdue ->
            add(
                AiBusinessInsight(
                    message = "Invoice for «${overdue.clientName}» is ${overdue.daysOverdue} days overdue — send a reminder.",
                    severity = if (overdue.daysOverdue > 14) AiInsightSeverity.CRITICAL else AiInsightSeverity.WARNING,
                ),
            )
        }

        if (summary.monthNetProfitCents < 0) {
            add(
                AiBusinessInsight(
                    message = "Net profit is negative — review major expense categories this period.",
                    severity = AiInsightSeverity.CRITICAL,
                ),
            )
        }

        if (summary.expectedReceivablesCents > summary.monthRevenueCents && summary.expectedReceivablesCents > 0) {
            add(
                AiBusinessInsight(
                    message = "Receivables exceed period revenue — accelerate collections on open invoices.",
                    severity = AiInsightSeverity.WARNING,
                ),
            )
        }

        val topExpense = expenseCategories.firstOrNull()
        if (topExpense != null && summary.monthExpenseCents > 0) {
            val share = (topExpense.amountCents.toDouble() / summary.monthExpenseCents * 100).toInt()
            if (share >= 25) {
                add(
                    AiBusinessInsight(
                        message = "Category «${topExpense.category}» is $share% of expenses — check for optimization.",
                        severity = AiInsightSeverity.INFO,
                    ),
                )
            }
        }

        if (isEmpty()) {
            add(
                AiBusinessInsight(
                    message = "Add clients, invoices, and transactions to unlock personalized insights.",
                    severity = AiInsightSeverity.INFO,
                ),
            )
        }
    }.take(5)
}
