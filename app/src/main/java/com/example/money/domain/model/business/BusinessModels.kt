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

data class BusinessDashboardSummary(
    val monthRevenueCents: Long,
    val monthExpenseCents: Long,
    val monthNetProfitCents: Long,
    val overdueReceivablesCents: Long,
    val expectedReceivablesCents: Long,
    val topClients: List<ClientRevenueRow>,
    val topExpenseCategories: List<CategoryAmountRow>,
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
