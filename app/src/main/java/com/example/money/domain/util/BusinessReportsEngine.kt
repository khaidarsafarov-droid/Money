package com.example.money.domain.util

import com.example.money.domain.model.business.BusinessClient
import com.example.money.domain.model.business.BusinessInvoice
import com.example.money.domain.model.business.BusinessLedgerType
import com.example.money.domain.model.business.BusinessProject
import com.example.money.domain.model.business.BusinessReportFilters
import com.example.money.domain.model.business.BusinessReportPeriod
import com.example.money.domain.model.business.BusinessReportsBundle
import com.example.money.domain.model.business.BusinessTransaction
import com.example.money.domain.model.business.BusinessTransactionType
import com.example.money.domain.model.business.CashflowReport
import com.example.money.domain.model.business.CategoryDistributionRow
import com.example.money.domain.model.business.CategoryReport
import com.example.money.domain.model.business.ClientReport
import com.example.money.domain.model.business.ClientReportRow
import com.example.money.domain.model.business.InvoiceStatus
import com.example.money.domain.model.business.MonthlyProfitPoint
import com.example.money.domain.model.business.ProfitLossReport
import com.example.money.domain.model.business.ProjectReport
import com.example.money.domain.model.business.ProjectReportRow
import com.example.money.domain.model.business.TaxReport
import com.example.money.domain.model.business.TaxReportLine
import java.util.Calendar
import java.util.concurrent.TimeUnit

object BusinessReportsEngine {

    fun periodRangeForReport(
        period: BusinessReportPeriod,
        referenceMillis: Long = System.currentTimeMillis(),
    ): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = referenceMillis }
        return when (period) {
            BusinessReportPeriod.DAY -> {
                val start = cal.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                start to start + TimeUnit.DAYS.toMillis(1)
            }
            BusinessReportPeriod.WEEK -> {
                val start = cal.apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                start to start + TimeUnit.DAYS.toMillis(7)
            }
            BusinessReportPeriod.MONTH -> BusinessFinanceEngine.monthRange(referenceMillis)
            BusinessReportPeriod.YEAR -> {
                val start = cal.apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val end = Calendar.getInstance().apply {
                    timeInMillis = start
                    add(Calendar.YEAR, 1)
                }.timeInMillis
                start to end
            }
        }
    }

    fun periodLabel(period: BusinessReportPeriod): String = when (period) {
        BusinessReportPeriod.DAY -> "Day"
        BusinessReportPeriod.WEEK -> "Week"
        BusinessReportPeriod.MONTH -> "Month"
        BusinessReportPeriod.YEAR -> "Year"
    }

    fun generateAll(
        transactions: List<BusinessTransaction>,
        invoices: List<BusinessInvoice>,
        clients: List<BusinessClient>,
        projects: List<BusinessProject>,
        filters: BusinessReportFilters,
        nowMillis: Long = System.currentTimeMillis(),
    ): BusinessReportsBundle = BusinessReportsBundle(
        profitLoss = generateProfitLossReport(transactions, filters, nowMillis),
        cashflow = generateCashflowReport(transactions, filters, nowMillis),
        client = generateClientReport(transactions, invoices, clients, filters, nowMillis),
        category = generateCategoryReport(transactions, filters, nowMillis),
        project = generateProjectReport(transactions, projects, clients, filters, nowMillis),
        tax = generateTaxReport(transactions, filters, nowMillis),
    )

    fun generateProfitLossReport(
        transactions: List<BusinessTransaction>,
        filters: BusinessReportFilters,
        nowMillis: Long = System.currentTimeMillis(),
    ): ProfitLossReport {
        val (start, end) = periodRangeForReport(filters.period, nowMillis)
        val periodTx = filterTransactions(transactions, filters, start, end)
        val revenue = sumType(periodTx, BusinessTransactionType.INCOME)
        val expense = sumType(periodTx, BusinessTransactionType.EXPENSE)
        val months = when (filters.period) {
            BusinessReportPeriod.YEAR -> 12
            BusinessReportPeriod.MONTH -> 6
            else -> 4
        }
        return ProfitLossReport(
            periodLabel = periodLabel(filters.period),
            revenueCents = revenue,
            expenseCents = expense,
            netProfitCents = revenue - expense,
            monthlyBreakdown = buildMonthlyBreakdown(transactions, filters, nowMillis, months),
        )
    }

    fun generateCashflowReport(
        transactions: List<BusinessTransaction>,
        filters: BusinessReportFilters,
        nowMillis: Long = System.currentTimeMillis(),
    ): CashflowReport {
        val (start, end) = periodRangeForReport(filters.period, nowMillis)
        val periodTx = filterTransactions(transactions, filters, start, end)
        val income = sumType(periodTx, BusinessTransactionType.INCOME)
        val expense = sumType(periodTx, BusinessTransactionType.EXPENSE)
        val weeks = when (filters.period) {
            BusinessReportPeriod.YEAR -> 12
            BusinessReportPeriod.MONTH -> 8
            else -> 4
        }
        return CashflowReport(
            periodLabel = periodLabel(filters.period),
            totalIncomeCents = income,
            totalExpenseCents = expense,
            netCashflowCents = income - expense,
            weeklyPoints = buildWeeklyPoints(filterTransactions(transactions, filters, 0, end), nowMillis, weeks),
        )
    }

    fun generateClientReport(
        transactions: List<BusinessTransaction>,
        invoices: List<BusinessInvoice>,
        clients: List<BusinessClient>,
        filters: BusinessReportFilters,
        nowMillis: Long = System.currentTimeMillis(),
    ): ClientReport {
        val (start, end) = periodRangeForReport(filters.period, nowMillis)
        val periodTx = filterTransactions(transactions, filters, start, end)
        val periodInvoices = filterInvoices(invoices, filters)
        val rows = clients.filter { it.isActive }.map { client ->
            val clientTx = periodTx.filter { it.clientId == client.id }
            val clientInvoices = periodInvoices.filter { it.clientId == client.id }
            val overdue = clientInvoices.count {
                (it.status == InvoiceStatus.SENT || it.status == InvoiceStatus.OVERDUE) &&
                    it.dueDate < nowMillis
            }
            val revenue = clientTx.filter { it.type == BusinessTransactionType.INCOME }.sumOf { it.amountCents }
            val expenses = clientTx.filter { it.type == BusinessTransactionType.EXPENSE }.sumOf { it.amountCents }
            ClientReportRow(
                clientId = client.id,
                clientName = client.name,
                revenueCents = revenue,
                expenseCents = expenses,
                profitCents = revenue - expenses,
                invoiceCount = clientInvoices.size,
                overdueCount = overdue,
            )
        }
            .filter { it.revenueCents > 0 || it.expenseCents > 0 || it.invoiceCount > 0 }
            .sortedByDescending { it.revenueCents }
        return ClientReport(periodLabel = periodLabel(filters.period), rows = rows)
    }

    fun generateCategoryReport(
        transactions: List<BusinessTransaction>,
        filters: BusinessReportFilters,
        nowMillis: Long = System.currentTimeMillis(),
    ): CategoryReport {
        val (start, end) = periodRangeForReport(filters.period, nowMillis)
        val periodTx = filterTransactions(transactions, filters, start, end)
        val rows = periodTx.groupBy { it.category }.map { (category, rows) ->
            CategoryDistributionRow(
                category = category,
                incomeCents = rows.filter { it.type == BusinessTransactionType.INCOME }.sumOf { it.amountCents },
                expenseCents = rows.filter { it.type == BusinessTransactionType.EXPENSE }.sumOf { it.amountCents },
            )
        }
            .filter { it.totalCents > 0 }
            .sortedByDescending { it.totalCents }
        return CategoryReport(periodLabel = periodLabel(filters.period), rows = rows)
    }

    fun generateProjectReport(
        transactions: List<BusinessTransaction>,
        projects: List<BusinessProject>,
        clients: List<BusinessClient>,
        filters: BusinessReportFilters,
        nowMillis: Long = System.currentTimeMillis(),
    ): ProjectReport {
        val (start, end) = periodRangeForReport(filters.period, nowMillis)
        val periodTx = filterTransactions(transactions, filters, start, end)
        val clientNames = clients.associate { it.id to it.name }
        val rows = projects.map { project ->
            val projectTx = periodTx.filter { it.projectId == project.id }
            val revenue = projectTx.filter { it.type == BusinessTransactionType.INCOME }.sumOf { it.amountCents }
            val expense = projectTx.filter { it.type == BusinessTransactionType.EXPENSE }.sumOf { it.amountCents }
            ProjectReportRow(
                projectId = project.id,
                projectName = project.name,
                clientName = clientNames[project.clientId] ?: "—",
                budgetCents = project.budgetCents,
                revenueCents = revenue,
                expenseCents = expense,
                profitCents = revenue - expense,
                status = project.status,
            )
        }
            .filter { it.revenueCents > 0 || it.expenseCents > 0 || it.budgetCents > 0 }
            .sortedByDescending { it.profitCents }
        return ProjectReport(periodLabel = periodLabel(filters.period), rows = rows)
    }

    fun generateTaxReport(
        transactions: List<BusinessTransaction>,
        filters: BusinessReportFilters,
        nowMillis: Long = System.currentTimeMillis(),
    ): TaxReport {
        val (start, end) = periodRangeForReport(filters.period, nowMillis)
        val periodTx = filterTransactions(transactions, filters, start, end)
            .filter { it.ledgerType != BusinessLedgerType.PERSONAL }
        val income = sumType(periodTx, BusinessTransactionType.INCOME)
        val deductible = periodTx
            .filter { it.type == BusinessTransactionType.EXPENSE && it.ledgerType != BusinessLedgerType.PERSONAL }
            .sumOf { it.amountCents }
        val taxLedger = periodTx
            .filter { it.ledgerType == BusinessLedgerType.TAX }
            .sumOf { it.amountCents }
        val lines = periodTx.sortedByDescending { it.date }.map { tx ->
            TaxReportLine(
                date = tx.date,
                category = tx.category,
                type = tx.type,
                amountCents = tx.amountCents,
                ledgerType = tx.ledgerType,
            )
        }
        return TaxReport(
            periodLabel = periodLabel(filters.period),
            taxableIncomeCents = income,
            deductibleExpensesCents = deductible,
            taxLedgerExpensesCents = taxLedger,
            netTaxableCents = income - deductible,
            lines = lines,
        )
    }

    private fun filterTransactions(
        transactions: List<BusinessTransaction>,
        filters: BusinessReportFilters,
        start: Long,
        end: Long,
    ): List<BusinessTransaction> = transactions.filter { tx ->
        tx.date >= start && tx.date < end &&
            tx.ledgerType != BusinessLedgerType.PERSONAL &&
            (filters.clientId == null || tx.clientId == filters.clientId) &&
            (filters.projectId == null || tx.projectId == filters.projectId) &&
            (filters.category == null || tx.category.equals(filters.category, ignoreCase = true))
    }

    private fun filterInvoices(
        invoices: List<BusinessInvoice>,
        filters: BusinessReportFilters,
    ): List<BusinessInvoice> = invoices.filter { inv ->
        (filters.clientId == null || inv.clientId == filters.clientId) &&
            (filters.projectId == null || inv.projectId == filters.projectId)
    }

    private fun sumType(transactions: List<BusinessTransaction>, type: BusinessTransactionType): Long =
        transactions.filter { it.type == type }.sumOf { it.amountCents }

    private fun buildMonthlyBreakdown(
        transactions: List<BusinessTransaction>,
        filters: BusinessReportFilters,
        nowMillis: Long,
        months: Int,
    ): List<MonthlyProfitPoint> {
        val businessTx = transactions.filter { it.ledgerType != BusinessLedgerType.PERSONAL }
        return (months - 1 downTo 0).map { index ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.MONTH, -index)
            }
            val (start, end) = BusinessFinanceEngine.monthRange(cal.timeInMillis)
            val monthTx = filterTransactions(businessTx, filters, start, end)
            val profit = sumType(monthTx, BusinessTransactionType.INCOME) -
                sumType(monthTx, BusinessTransactionType.EXPENSE)
            MonthlyProfitPoint(
                label = "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.YEAR) % 100}",
                profitCents = profit,
            )
        }
    }

    private fun buildWeeklyPoints(
        transactions: List<BusinessTransaction>,
        nowMillis: Long,
        weeks: Int,
    ): List<com.example.money.domain.model.business.CashflowWeekPoint> {
        val businessTx = transactions.filter { it.ledgerType != BusinessLedgerType.PERSONAL }
        return (weeks - 1 downTo 0).map { index ->
            val weekEnd = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.WEEK_OF_YEAR, -index)
                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val weekStartMillis = weekEnd.timeInMillis
            val weekEndMillis = weekStartMillis + TimeUnit.DAYS.toMillis(7)
            val weekTx = businessTx.filter { it.date >= weekStartMillis && it.date < weekEndMillis }
            com.example.money.domain.model.business.CashflowWeekPoint(
                label = "W${weeks - index}",
                incomeCents = sumType(weekTx, BusinessTransactionType.INCOME),
                expenseCents = sumType(weekTx, BusinessTransactionType.EXPENSE),
            )
        }
    }
}
