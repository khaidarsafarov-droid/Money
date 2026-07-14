package com.example.money.domain.model.business

enum class BusinessReportType {
    PROFIT_LOSS,
    CASHFLOW,
    CLIENT,
    CATEGORY,
    PROJECT,
    TAX,
}

enum class BusinessReportPeriod {
    DAY,
    WEEK,
    MONTH,
    YEAR,
}

data class BusinessReportFilters(
    val period: BusinessReportPeriod = BusinessReportPeriod.MONTH,
    val clientId: Long? = null,
    val category: String? = null,
    val projectId: Long? = null,
)

data class ProfitLossReport(
    val periodLabel: String,
    val revenueCents: Long,
    val expenseCents: Long,
    val netProfitCents: Long,
    val monthlyBreakdown: List<MonthlyProfitPoint>,
)

data class CashflowReport(
    val periodLabel: String,
    val totalIncomeCents: Long,
    val totalExpenseCents: Long,
    val netCashflowCents: Long,
    val weeklyPoints: List<CashflowWeekPoint>,
)

data class ClientReportRow(
    val clientId: Long,
    val clientName: String,
    val revenueCents: Long,
    val expenseCents: Long,
    val profitCents: Long,
    val invoiceCount: Int,
    val overdueCount: Int,
)

data class ClientReport(
    val periodLabel: String,
    val rows: List<ClientReportRow>,
)

data class CategoryReport(
    val periodLabel: String,
    val rows: List<CategoryDistributionRow>,
)

data class ProjectReportRow(
    val projectId: Long,
    val projectName: String,
    val clientName: String,
    val budgetCents: Long,
    val revenueCents: Long,
    val expenseCents: Long,
    val profitCents: Long,
    val status: ProjectStatus,
)

data class ProjectReport(
    val periodLabel: String,
    val rows: List<ProjectReportRow>,
)

data class TaxReportLine(
    val date: Long,
    val category: String,
    val type: BusinessTransactionType,
    val amountCents: Long,
    val ledgerType: BusinessLedgerType,
)

data class TaxReport(
    val periodLabel: String,
    val taxableIncomeCents: Long,
    val deductibleExpensesCents: Long,
    val taxLedgerExpensesCents: Long,
    val netTaxableCents: Long,
    val lines: List<TaxReportLine>,
)

data class BusinessReportsBundle(
    val profitLoss: ProfitLossReport,
    val cashflow: CashflowReport,
    val client: ClientReport,
    val category: CategoryReport,
    val project: ProjectReport,
    val tax: TaxReport,
)
