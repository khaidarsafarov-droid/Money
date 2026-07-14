package com.example.money.domain.util.ai

import com.example.money.domain.model.business.BusinessClient
import com.example.money.domain.model.business.BusinessDashboardSummary
import com.example.money.domain.model.business.BusinessInvoice
import com.example.money.domain.model.business.BusinessTransaction
import com.example.money.domain.model.business.BusinessTransactionType
import com.example.money.domain.model.business.CashflowForecastPoint
import com.example.money.domain.model.business.InvoiceRiskAssessment
import com.example.money.domain.model.business.InvoiceStatus
import com.example.money.ui.util.Formatters
import java.util.concurrent.TimeUnit

object BusinessAiAssistantService {

    fun respond(
        promptId: String,
        dashboard: BusinessDashboardSummary,
        invoices: List<BusinessInvoice>,
        clients: List<BusinessClient>,
        transactions: List<BusinessTransaction>,
        forecast: List<CashflowForecastPoint>,
        invoiceRisks: Map<Long, InvoiceRiskAssessment>,
    ): String = when (promptId) {
        "profit_summary" -> profitSummary(dashboard)
        "cashflow_forecast" -> cashflowForecast(forecast)
        "overdue_invoices" -> overdueInvoices(invoices, clients)
        "top_clients" -> topClients(dashboard)
        "insights" -> insights(dashboard)
        "invoice_risks" -> invoiceRisks(invoices, clients, invoiceRisks)
        "category_hint" -> categoryHint(transactions)
        else -> dashboard.aiInsights.firstOrNull()?.message
            ?: "Ask about profit, cashflow forecast, overdue invoices, or top clients."
    }

    private fun profitSummary(summary: BusinessDashboardSummary): String {
        val revenue = Formatters.currencyFromCents(summary.monthRevenueCents)
        val expenses = Formatters.currencyFromCents(summary.monthExpenseCents)
        val profit = Formatters.currencyFromCents(summary.monthNetProfitCents)
        val margin = if (summary.monthRevenueCents > 0) {
            (summary.monthNetProfitCents.toDouble() / summary.monthRevenueCents * 100).toInt()
        } else {
            0
        }
        return buildString {
            append("Period summary:\n")
            append("• Revenue: $revenue\n")
            append("• Expenses: $expenses\n")
            append("• Net profit: $profit ($margin% margin)\n")
            append("• Cash on hand: ${Formatters.currencyFromCents(summary.cashOnHandCents)}\n")
            if (summary.overdueReceivablesCents > 0) {
                append("• Overdue receivables: ${Formatters.currencyFromCents(summary.overdueReceivablesCents)}")
            }
        }
    }

    private fun cashflowForecast(forecast: List<CashflowForecastPoint>): String {
        if (forecast.isEmpty()) return "Not enough transaction history for a forecast yet."
        val endBalance = forecast.last().projectedBalanceCents
        val minPoint = forecast.minByOrNull { it.projectedBalanceCents }
        return buildString {
            append("30-day cashflow forecast:\n")
            append("• Projected balance: ${Formatters.currencyFromCents(endBalance)}\n")
            minPoint?.let { low ->
                if (low.projectedBalanceCents < 0) {
                    append("• Warning: balance may drop to ${Formatters.currencyFromCents(low.projectedBalanceCents)} around ${low.label}")
                } else {
                    append("• Lowest projected balance: ${Formatters.currencyFromCents(low.projectedBalanceCents)}")
                }
            }
        }
    }

    private fun overdueInvoices(
        invoices: List<BusinessInvoice>,
        clients: List<BusinessClient>,
    ): String {
        val now = System.currentTimeMillis()
        val overdue = invoices.filter {
            it.status != InvoiceStatus.PAID && it.dueDate < now
        }
        if (overdue.isEmpty()) return "No overdue invoices — collections look healthy."
        val total = overdue.sumOf { it.amountCents }
        val lines = overdue.take(5).joinToString("\n") { inv ->
            val name = clients.find { it.id == inv.clientId }?.name ?: "Client #${inv.clientId}"
            val days = TimeUnit.MILLISECONDS.toDays(now - inv.dueDate).coerceAtLeast(1)
            "• $name — ${Formatters.currencyFromCents(inv.amountCents)} ($days days overdue)"
        }
        return "Overdue invoices (${Formatters.currencyFromCents(total)} total):\n$lines"
    }

    private fun topClients(summary: BusinessDashboardSummary): String {
        if (summary.topClients.isEmpty()) return "No client revenue data for this period."
        return summary.topClients.take(5).joinToString("\n", "Top clients by revenue:\n") { row ->
            "• ${row.clientName} — ${Formatters.currencyFromCents(row.revenueCents)}"
        }
    }

    private fun insights(summary: BusinessDashboardSummary): String {
        val insights = summary.aiInsights
        if (insights.isEmpty()) return "No notable insights for the current period."
        return insights.joinToString("\n\n") { "• ${it.message}" }
    }

    private fun invoiceRisks(
        invoices: List<BusinessInvoice>,
        clients: List<BusinessClient>,
        risks: Map<Long, InvoiceRiskAssessment>,
    ): String {
        val open = invoices.filter { it.status != InvoiceStatus.PAID }
        val highRisk = open.filter { risks[it.id]?.probabilityPercent ?: 0 >= 70 }
        if (highRisk.isEmpty()) return "No high-risk open invoices detected."
        return highRisk.take(5).joinToString("\n", "High-risk invoices:\n") { inv ->
            val name = clients.find { it.id == inv.clientId }?.name ?: "Client #${inv.clientId}"
            val risk = risks[inv.id]
            "• $name — ${Formatters.currencyFromCents(inv.amountCents)} (risk ${risk?.probabilityPercent ?: 0}%)"
        }
    }

    private fun categoryHint(transactions: List<BusinessTransaction>): String {
        val recent = transactions.maxByOrNull { it.date }
            ?: return "Add transactions to get category suggestions."
        val suggestion = AiCategorySuggestionService.suggest(
            note = recent.note,
            type = recent.type,
            clientId = recent.clientId,
            history = transactions,
            customCategories = emptyList(),
        )
        return if (suggestion != null) {
            "For a similar ${recent.type.name.lowercase()} transaction, suggested category: «$suggestion»"
        } else {
            "No strong category match from history — use your most common expense categories."
        }
    }
}
