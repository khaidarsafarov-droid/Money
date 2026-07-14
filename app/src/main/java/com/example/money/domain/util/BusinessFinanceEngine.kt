package com.example.money.domain.util

import com.example.money.domain.model.business.BusinessClient
import com.example.money.domain.model.business.BusinessDashboardFilters
import com.example.money.domain.model.business.BusinessDashboardSummary
import com.example.money.domain.model.business.BusinessInvoice
import com.example.money.domain.model.business.BusinessLedgerType
import com.example.money.domain.model.business.BusinessProject
import com.example.money.domain.model.business.BusinessTransaction
import com.example.money.domain.model.business.BusinessTransactionType
import com.example.money.domain.model.business.CashflowWeekPoint
import com.example.money.domain.model.business.ClientProfitRow
import com.example.money.domain.model.business.ClientRevenueRow
import com.example.money.domain.model.business.CategoryAmountRow
import com.example.money.domain.model.business.InvoiceStatus
import com.example.money.domain.model.business.MonthlyProfitPoint
import com.example.money.domain.model.business.OverdueInvoiceRow
import com.example.money.domain.model.business.ProjectPerformanceRow
import com.example.money.domain.model.business.ProjectStatus
import com.example.money.domain.model.business.UpcomingPaymentRow
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

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

    fun previousMonthRange(referenceMillis: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = referenceMillis }
        cal.add(Calendar.MONTH, -1)
        return monthRange(cal.timeInMillis)
    }

    fun computeDashboard(
        transactions: List<BusinessTransaction>,
        invoices: List<BusinessInvoice>,
        clients: List<BusinessClient>,
        projects: List<BusinessProject>,
        monthStartMillis: Long,
        monthEndMillis: Long,
        filters: BusinessDashboardFilters = BusinessDashboardFilters(),
        nowMillis: Long = System.currentTimeMillis(),
    ): BusinessDashboardSummary {
        val filteredTx = applyFilters(transactions, filters)
        val filteredInvoices = applyInvoiceFilters(invoices, filters)

        val monthTx = filteredTx.filter { tx ->
            tx.date >= monthStartMillis && tx.date < monthEndMillis &&
                tx.ledgerType != BusinessLedgerType.PERSONAL
        }
        val (prevStart, prevEnd) = previousMonthRange(monthEndMillis - 1)
        val prevMonthTx = filteredTx.filter { tx ->
            tx.date >= prevStart && tx.date < prevEnd &&
                tx.ledgerType != BusinessLedgerType.PERSONAL
        }

        val revenueCents = sumIncome(monthTx)
        val expenseCents = sumExpense(monthTx)
        val prevRevenue = sumIncome(prevMonthTx)
        val prevExpense = sumExpense(prevMonthTx)
        val prevProfit = prevRevenue - prevExpense
        val netProfit = revenueCents - expenseCents

        val allTimeIncome = sumIncome(filteredTx.filter { it.ledgerType != BusinessLedgerType.PERSONAL })
        val allTimeExpense = sumExpense(filteredTx.filter { it.ledgerType != BusinessLedgerType.PERSONAL })
        val cashOnHand = allTimeIncome - allTimeExpense

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

        val openInvoices = filteredInvoices.filter {
            it.status == InvoiceStatus.SENT || it.status == InvoiceStatus.OVERDUE
        }
        val overdueReceivables = openInvoices
            .filter { it.dueDate < nowMillis }
            .sumOf { it.amountCents }
        val expectedReceivables = openInvoices.sumOf { it.amountCents }

        val weeklyPoints = buildWeeklyCashflow(filteredTx, nowMillis, weeks = 8)
        val monthlyProfit = buildMonthlyProfit(filteredTx, nowMillis, months = 6)
        val sparkWeeks = buildWeeklyCashflow(filteredTx, nowMillis, weeks = 6)
        var runningCash = 0L
        val cashSparkline = sparkWeeks.map { point ->
            runningCash += point.incomeCents - point.expenseCents
            runningCash
        }

        return BusinessDashboardSummary(
            monthRevenueCents = revenueCents,
            monthExpenseCents = expenseCents,
            monthNetProfitCents = netProfit,
            overdueReceivablesCents = overdueReceivables,
            expectedReceivablesCents = expectedReceivables,
            topClients = revenueByClient,
            topExpenseCategories = expenseByCategory,
            cashOnHandCents = cashOnHand,
            revenueChangePercent = percentChange(revenueCents, prevRevenue),
            expenseChangePercent = percentChange(expenseCents, prevExpense),
            profitChangePercent = percentChange(netProfit, prevProfit),
            cashChangePercent = percentChange(netProfit, prevProfit),
            revenueSparkline = sparkWeeks.map { it.incomeCents },
            expenseSparkline = sparkWeeks.map { it.expenseCents },
            profitSparkline = sparkWeeks.map { it.incomeCents - it.expenseCents },
            cashSparkline = cashSparkline,
            cashflowWeekly = weeklyPoints,
            monthlyProfit = monthlyProfit,
            clientTable = buildClientTable(filteredTx, filteredInvoices, clients, monthStartMillis, monthEndMillis),
            projectCards = buildProjectCards(filteredTx, projects, nowMillis),
            overdueInvoices = buildOverdueInvoices(filteredInvoices, clients, nowMillis),
            upcomingPayments = buildUpcomingPayments(filteredInvoices, clients, nowMillis),
            insights = BusinessInsightEngine.generate(
                summary = BusinessDashboardSummary(
                    monthRevenueCents = revenueCents,
                    monthExpenseCents = expenseCents,
                    monthNetProfitCents = netProfit,
                    overdueReceivablesCents = overdueReceivables,
                    expectedReceivablesCents = expectedReceivables,
                    topClients = revenueByClient,
                    topExpenseCategories = expenseByCategory,
                ),
                clientTable = buildClientTable(filteredTx, filteredInvoices, clients, monthStartMillis, monthEndMillis),
                overdueInvoices = buildOverdueInvoices(filteredInvoices, clients, nowMillis),
                expenseCategories = expenseByCategory,
            ),
        )
    }

    fun resolveInvoiceStatus(invoice: BusinessInvoice, nowMillis: Long): InvoiceStatus {
        if (invoice.status == InvoiceStatus.PAID || invoice.status == InvoiceStatus.DRAFT) {
            return invoice.status
        }
        return if (invoice.dueDate < nowMillis) InvoiceStatus.OVERDUE else invoice.status
    }

    private fun applyFilters(
        transactions: List<BusinessTransaction>,
        filters: BusinessDashboardFilters,
    ): List<BusinessTransaction> = transactions.filter { tx ->
        (filters.clientId == null || tx.clientId == filters.clientId) &&
            (filters.projectId == null || tx.projectId == filters.projectId) &&
            (filters.transactionType == null || tx.type == filters.transactionType)
    }

    private fun applyInvoiceFilters(
        invoices: List<BusinessInvoice>,
        filters: BusinessDashboardFilters,
    ): List<BusinessInvoice> = invoices.filter { inv ->
        (filters.clientId == null || inv.clientId == filters.clientId) &&
            (filters.projectId == null || inv.projectId == filters.projectId) &&
            (filters.invoiceStatus == null || inv.status == filters.invoiceStatus)
    }

    private fun sumIncome(transactions: List<BusinessTransaction>): Long =
        transactions.filter { it.type == BusinessTransactionType.INCOME }.sumOf { it.amountCents }

    private fun sumExpense(transactions: List<BusinessTransaction>): Long =
        transactions.filter { it.type == BusinessTransactionType.EXPENSE }.sumOf { it.amountCents }

    private fun percentChange(current: Long, previous: Long): Int? {
        if (previous == 0L) return if (current == 0L) null else 100
        return ((current - previous).toDouble() / previous * 100).roundToInt()
    }

    private fun buildWeeklyCashflow(
        transactions: List<BusinessTransaction>,
        nowMillis: Long,
        weeks: Int,
    ): List<CashflowWeekPoint> {
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
            val label = "W${weeks - index}"
            val weekTx = businessTx.filter { it.date >= weekStartMillis && it.date < weekEndMillis }
            CashflowWeekPoint(
                label = label,
                incomeCents = sumIncome(weekTx),
                expenseCents = sumExpense(weekTx),
            )
        }
    }

    private fun buildMonthlyProfit(
        transactions: List<BusinessTransaction>,
        nowMillis: Long,
        months: Int,
    ): List<MonthlyProfitPoint> {
        val businessTx = transactions.filter { it.ledgerType != BusinessLedgerType.PERSONAL }
        return (months - 1 downTo 0).map { index ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.MONTH, -index)
            }
            val (start, end) = monthRange(cal.timeInMillis)
            val monthTx = businessTx.filter { it.date >= start && it.date < end }
            val profit = sumIncome(monthTx) - sumExpense(monthTx)
            val label = "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.YEAR) % 100}"
            MonthlyProfitPoint(label = label, profitCents = profit)
        }
    }

    private fun buildClientTable(
        transactions: List<BusinessTransaction>,
        invoices: List<BusinessInvoice>,
        clients: List<BusinessClient>,
        monthStart: Long,
        monthEnd: Long,
    ): List<ClientProfitRow> {
        val monthTx = transactions.filter { it.date >= monthStart && it.date < monthEnd }
        return clients.filter { it.isActive }.map { client ->
            val clientTx = monthTx.filter { it.clientId == client.id }
            val revenue = clientTx.filter { it.type == BusinessTransactionType.INCOME }.sumOf { it.amountCents }
            val expenses = clientTx.filter { it.type == BusinessTransactionType.EXPENSE }.sumOf { it.amountCents }
            val lastInvoice = invoices.filter { it.clientId == client.id }.maxByOrNull { it.issueDate }
            ClientProfitRow(
                clientId = client.id,
                clientName = client.name,
                revenueCents = revenue,
                profitCents = revenue - expenses,
                lastInvoiceStatus = lastInvoice?.status,
            )
        }
            .filter { it.revenueCents > 0 || it.profitCents != 0L }
            .sortedByDescending { it.revenueCents }
            .take(5)
    }

    private fun buildProjectCards(
        transactions: List<BusinessTransaction>,
        projects: List<BusinessProject>,
        nowMillis: Long,
    ): List<ProjectPerformanceRow> =
        projects.map { project ->
            val actual = transactions
                .filter { it.projectId == project.id && it.type == BusinessTransactionType.EXPENSE }
                .sumOf { it.amountCents }
            val budget = project.budgetCents.coerceAtLeast(1)
            val progress = (actual.toFloat() / budget).coerceIn(0f, 1.5f)
            val status = when {
                project.status == ProjectStatus.COMPLETED -> ProjectStatus.COMPLETED
                project.status == ProjectStatus.ON_HOLD -> ProjectStatus.ON_HOLD
                actual > budget -> ProjectStatus.ACTIVE
                else -> project.status
            }
            ProjectPerformanceRow(
                projectId = project.id,
                projectName = project.name,
                budgetCents = project.budgetCents,
                actualCents = actual,
                status = status,
                progressFraction = progress,
            )
        }.take(5)

    private fun buildOverdueInvoices(
        invoices: List<BusinessInvoice>,
        clients: List<BusinessClient>,
        nowMillis: Long,
    ): List<OverdueInvoiceRow> {
        val names = clients.associate { it.id to it.name }
        return invoices
            .filter {
                (it.status == InvoiceStatus.SENT || it.status == InvoiceStatus.OVERDUE) &&
                    it.dueDate < nowMillis
            }
            .map { inv ->
                val days = TimeUnit.MILLISECONDS.toDays(nowMillis - inv.dueDate).coerceAtLeast(1)
                OverdueInvoiceRow(
                    invoiceId = inv.id,
                    clientName = names[inv.clientId] ?: "—",
                    amountCents = inv.amountCents,
                    daysOverdue = days,
                )
            }
            .sortedByDescending { it.daysOverdue }
            .take(5)
    }

    private fun buildUpcomingPayments(
        invoices: List<BusinessInvoice>,
        clients: List<BusinessClient>,
        nowMillis: Long,
    ): List<UpcomingPaymentRow> {
        val horizon = nowMillis + TimeUnit.DAYS.toMillis(30)
        val names = clients.associate { it.id to it.name }
        return invoices
            .filter {
                it.status == InvoiceStatus.SENT &&
                    it.dueDate >= nowMillis &&
                    it.dueDate <= horizon
            }
            .sortedBy { it.dueDate }
            .take(5)
            .map { inv ->
                UpcomingPaymentRow(
                    invoiceId = inv.id,
                    clientName = names[inv.clientId] ?: "—",
                    amountCents = inv.amountCents,
                    dueDate = inv.dueDate,
                )
            }
    }
}
