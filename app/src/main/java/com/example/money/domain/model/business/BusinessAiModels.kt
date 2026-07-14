package com.example.money.domain.model.business

enum class AiInsightSeverity {
    INFO,
    WARNING,
    CRITICAL,
}

data class AiBusinessInsight(
    val message: String,
    val severity: AiInsightSeverity,
)

enum class InvoiceRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
}

data class InvoiceRiskAssessment(
    val invoiceId: Long,
    val riskLevel: InvoiceRiskLevel,
    val probabilityPercent: Int,
)

data class CashflowForecastPoint(
    val label: String,
    val projectedIncomeCents: Long,
    val projectedExpenseCents: Long,
    val projectedBalanceCents: Long,
)
