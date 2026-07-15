package com.example.money.business.automation

import com.example.money.domain.model.business.BusinessCommunicationSendResult

data class BusinessAutomationRunResult(
    val workflowDebts: BusinessCommunicationSendResult,
    val overdueInvoices: Int,
    val largeTransactionAlerts: Int,
    val weeklyInsightShown: Boolean,
)
