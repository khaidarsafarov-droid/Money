package com.example.money.domain.util

import com.example.money.domain.model.business.BusinessReportType
import com.example.money.domain.model.business.BusinessReportsBundle
import com.example.money.domain.model.business.BusinessTransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BusinessCsvExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun export(type: BusinessReportType, bundle: BusinessReportsBundle): String = when (type) {
        BusinessReportType.PROFIT_LOSS -> exportProfitLoss(bundle.profitLoss)
        BusinessReportType.CASHFLOW -> exportCashflow(bundle.cashflow)
        BusinessReportType.CLIENT -> exportClient(bundle.client)
        BusinessReportType.CATEGORY -> exportCategory(bundle.category)
        BusinessReportType.PROJECT -> exportProject(bundle.project)
        BusinessReportType.TAX -> exportTax(bundle.tax)
    }

    fun fileName(type: BusinessReportType): String = when (type) {
        BusinessReportType.PROFIT_LOSS -> "profit_loss_report.csv"
        BusinessReportType.CASHFLOW -> "cashflow_report.csv"
        BusinessReportType.CLIENT -> "client_report.csv"
        BusinessReportType.CATEGORY -> "category_report.csv"
        BusinessReportType.PROJECT -> "project_report.csv"
        BusinessReportType.TAX -> "tax_report.csv"
    }

    private fun exportProfitLoss(report: com.example.money.domain.model.business.ProfitLossReport): String = buildString {
        appendLine("Profit & Loss Report,${report.periodLabel}")
        appendLine("Revenue,${cents(report.revenueCents)}")
        appendLine("Expenses,${cents(report.expenseCents)}")
        appendLine("Net Profit,${cents(report.netProfitCents)}")
        appendLine()
        appendLine("Month,Profit")
        report.monthlyBreakdown.forEach { appendLine("${it.label},${cents(it.profitCents)}") }
    }

    private fun exportCashflow(report: com.example.money.domain.model.business.CashflowReport): String = buildString {
        appendLine("Cashflow Report,${report.periodLabel}")
        appendLine("Income,${cents(report.totalIncomeCents)}")
        appendLine("Expenses,${cents(report.totalExpenseCents)}")
        appendLine("Net Cashflow,${cents(report.netCashflowCents)}")
        appendLine()
        appendLine("Period,Income,Expenses")
        report.weeklyPoints.forEach {
            appendLine("${it.label},${cents(it.incomeCents)},${cents(it.expenseCents)}")
        }
    }

    private fun exportClient(report: com.example.money.domain.model.business.ClientReport): String = buildString {
        appendLine("Client Report,${report.periodLabel}")
        appendLine("Client,Revenue,Expenses,Profit,Invoices,Overdue")
        report.rows.forEach {
            appendLine(
                "${csvEscape(it.clientName)},${cents(it.revenueCents)},${cents(it.expenseCents)}," +
                    "${cents(it.profitCents)},${it.invoiceCount},${it.overdueCount}",
            )
        }
    }

    private fun exportCategory(report: com.example.money.domain.model.business.CategoryReport): String = buildString {
        appendLine("Category Report,${report.periodLabel}")
        appendLine("Category,Income,Expenses,Total")
        report.rows.forEach {
            appendLine(
                "${csvEscape(it.category)},${cents(it.incomeCents)},${cents(it.expenseCents)},${cents(it.totalCents)}",
            )
        }
    }

    private fun exportProject(report: com.example.money.domain.model.business.ProjectReport): String = buildString {
        appendLine("Project Report,${report.periodLabel}")
        appendLine("Project,Client,Budget,Revenue,Expenses,Profit,Status")
        report.rows.forEach {
            appendLine(
                "${csvEscape(it.projectName)},${csvEscape(it.clientName)},${cents(it.budgetCents)}," +
                    "${cents(it.revenueCents)},${cents(it.expenseCents)},${cents(it.profitCents)},${it.status.name}",
            )
        }
    }

    private fun exportTax(report: com.example.money.domain.model.business.TaxReport): String = buildString {
        appendLine("Tax Report,${report.periodLabel}")
        appendLine("Taxable Income,${cents(report.taxableIncomeCents)}")
        appendLine("Deductible Expenses,${cents(report.deductibleExpensesCents)}")
        appendLine("Tax Ledger Expenses,${cents(report.taxLedgerExpensesCents)}")
        appendLine("Net Taxable,${cents(report.netTaxableCents)}")
        appendLine()
        appendLine("Date,Category,Type,Amount,Ledger")
        report.lines.forEach {
            appendLine(
                "${dateFormat.format(Date(it.date))},${csvEscape(it.category)},${it.type.name}," +
                    "${cents(it.amountCents)},${it.ledgerType.name}",
            )
        }
    }

    private fun cents(value: Long): String = "%.2f".format(Locale.US, value / 100.0)

    private fun csvEscape(value: String): String =
        if (value.contains(',') || value.contains('"')) "\"${value.replace("\"", "\"\"")}\"" else value
}
