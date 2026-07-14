package com.example.money.domain.model.business

enum class InvoiceStatus {
    DRAFT,
    SENT,
    PAID,
    OVERDUE,
}

enum class ProjectStatus {
    ACTIVE,
    COMPLETED,
    ON_HOLD,
    CANCELLED,
}

enum class BusinessTransactionType {
    INCOME,
    EXPENSE,
}

enum class BusinessLedgerType {
    OPERATING,
    INVESTMENT,
    TAX,
    PERSONAL,
}

data class BusinessClient(
    val id: Long = 0,
    val syncId: String = "",
    val name: String,
    val contactInfo: String = "",
    val email: String = "",
    val phone: String = "",
    val isActive: Boolean = true,
    val note: String = "",
)

data class BusinessProject(
    val id: Long = 0,
    val syncId: String = "",
    val clientId: Long,
    val name: String,
    val status: ProjectStatus = ProjectStatus.ACTIVE,
    val budgetCents: Long = 0,
    val note: String = "",
)

data class BusinessInvoice(
    val id: Long = 0,
    val syncId: String = "",
    val clientId: Long,
    val projectId: Long? = null,
    val issueDate: Long,
    val dueDate: Long,
    val amountCents: Long,
    val status: InvoiceStatus = InvoiceStatus.DRAFT,
    val note: String = "",
)

data class BusinessTransaction(
    val id: Long = 0,
    val syncId: String = "",
    val type: BusinessTransactionType,
    val amountCents: Long,
    val category: String,
    val ledgerType: BusinessLedgerType = BusinessLedgerType.OPERATING,
    val clientId: Long? = null,
    val projectId: Long? = null,
    val invoiceId: Long? = null,
    val date: Long,
    val note: String = "",
)

enum class BusinessDashboardView {
    FINANCIAL,
    CLIENT,
    PROJECT,
}

enum class BusinessFilterPreset {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    BY_CLIENT,
    BY_PROJECT,
}

enum class BusinessAttentionSeverity {
    INFO,
    WARNING,
    CRITICAL,
}

enum class BusinessAttentionType {
    OVERDUE_INVOICE,
    LOW_CASH,
    LARGE_EXPENSE,
}

data class BusinessAttentionAlert(
    val type: BusinessAttentionType,
    val severity: BusinessAttentionSeverity,
    val clientName: String? = null,
    val daysOverdue: Long? = null,
    val amountCents: Long? = null,
    val category: String? = null,
)

data class SavedFilterPreset(
    val name: String,
    val clientId: Long? = null,
    val projectId: Long? = null,
    val invoiceStatus: InvoiceStatus? = null,
    val periodPreset: BusinessFilterPreset = BusinessFilterPreset.THIS_MONTH,
)

data class BusinessDashboardSummary(
    val monthRevenueCents: Long,
    val monthExpenseCents: Long,
    val monthNetProfitCents: Long,
    val overdueReceivablesCents: Long,
    val expectedReceivablesCents: Long,
    val topClients: List<ClientRevenueRow>,
    val topExpenseCategories: List<CategoryAmountRow>,
    val cashOnHandCents: Long = 0,
    val revenueChangePercent: Int? = null,
    val expenseChangePercent: Int? = null,
    val profitChangePercent: Int? = null,
    val cashChangePercent: Int? = null,
    val revenueSparkline: List<Long> = emptyList(),
    val expenseSparkline: List<Long> = emptyList(),
    val profitSparkline: List<Long> = emptyList(),
    val cashSparkline: List<Long> = emptyList(),
    val cashflowWeekly: List<CashflowWeekPoint> = emptyList(),
    val monthlyProfit: List<MonthlyProfitPoint> = emptyList(),
    val clientTable: List<ClientProfitRow> = emptyList(),
    val projectCards: List<ProjectPerformanceRow> = emptyList(),
    val overdueInvoices: List<OverdueInvoiceRow> = emptyList(),
    val upcomingPayments: List<UpcomingPaymentRow> = emptyList(),
    val insights: List<String> = emptyList(),
    val attentionAlerts: List<BusinessAttentionAlert> = emptyList(),
)

data class CashflowWeekPoint(
    val label: String,
    val incomeCents: Long,
    val expenseCents: Long,
)

data class MonthlyProfitPoint(
    val label: String,
    val profitCents: Long,
)

data class ClientProfitRow(
    val clientId: Long,
    val clientName: String,
    val revenueCents: Long,
    val profitCents: Long,
    val lastInvoiceStatus: InvoiceStatus?,
)

data class ProjectPerformanceRow(
    val projectId: Long,
    val projectName: String,
    val budgetCents: Long,
    val actualCents: Long,
    val status: ProjectStatus,
    val progressFraction: Float,
)

data class OverdueInvoiceRow(
    val invoiceId: Long,
    val clientName: String,
    val amountCents: Long,
    val daysOverdue: Long,
)

data class UpcomingPaymentRow(
    val invoiceId: Long,
    val clientName: String,
    val amountCents: Long,
    val dueDate: Long,
)

data class BusinessDashboardFilters(
    val clientId: Long? = null,
    val projectId: Long? = null,
    val periodPreset: BusinessFilterPreset = BusinessFilterPreset.THIS_MONTH,
    val transactionType: BusinessTransactionType? = null,
    val invoiceStatus: InvoiceStatus? = null,
)

data class ClientRevenueRow(
    val clientId: Long,
    val clientName: String,
    val revenueCents: Long,
)

data class CategoryAmountRow(
    val category: String,
    val amountCents: Long,
)

object BusinessCategories {
    val revenue = listOf("Revenue", "Services", "Products", "Consulting", "Other income")
    val expenses = listOf(
        "COGS",
        "Marketing",
        "Software",
        "Travel",
        "Taxes",
        "Rent",
        "Payroll",
        "Utilities",
        "Other expense",
    )
    val all = revenue + expenses
}
