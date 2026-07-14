package com.example.money.business.sms

import com.example.money.domain.model.business.BusinessClient
import com.example.money.domain.model.business.BusinessInvoice
import com.example.money.domain.model.business.BusinessProject
import com.example.money.domain.model.business.BusinessSmsTemplate
import com.example.money.domain.model.business.BusinessSmsTemplateId
import com.example.money.domain.model.business.BusinessTransaction
import com.example.money.domain.model.business.InvoiceStatus
import com.example.money.ui.util.Formatters

object BusinessSmsTemplates {

    val all: List<BusinessSmsTemplate> = BusinessSmsTemplateId.entries
        .filter { it != BusinessSmsTemplateId.CUSTOM }
        .map { id ->
            BusinessSmsTemplate(
                id = id,
                titleResKey = "business_sms_template_${id.name.lowercase()}",
                bodyPattern = defaultPattern(id),
            )
        }

    fun render(
        templateId: BusinessSmsTemplateId,
        context: BusinessSmsTemplateContext,
    ): String {
        val pattern = all.find { it.id == templateId }?.bodyPattern ?: defaultPattern(templateId)
        return pattern
            .replace("{client}", context.clientName)
            .replace("{business}", context.businessName)
            .replace("{amount}", context.amountLabel)
            .replace("{due_date}", context.dueDateLabel)
            .replace("{invoice_status}", context.invoiceStatusLabel)
            .replace("{project}", context.projectName)
            .replace("{product}", context.productName)
            .replace("{meeting_time}", context.meetingTimeLabel)
            .replace("{subscription_date}", context.subscriptionDateLabel)
            .trim()
    }

    private fun defaultPattern(id: BusinessSmsTemplateId): String = when (id) {
        BusinessSmsTemplateId.DEBT_REMINDER ->
            "Hello {client}, this is {business}. Friendly reminder about outstanding balance of {amount}. Please contact us if you have questions."
        BusinessSmsTemplateId.INVOICE_STATUS ->
            "Hello {client}, invoice status from {business}: {invoice_status}. Amount: {amount}. Due: {due_date}."
        BusinessSmsTemplateId.NEW_PRODUCT ->
            "Hello {client}, {business} has a new offering: {product}. Reply if you want details."
        BusinessSmsTemplateId.PROMOTION ->
            "Hello {client}, {business} promotion: {product}. Limited time offer — contact us to learn more."
        BusinessSmsTemplateId.PROJECT_UPDATE ->
            "Hello {client}, project update from {business}: {project} — {invoice_status}."
        BusinessSmsTemplateId.DELIVERY_UPDATE ->
            "Hello {client}, {business} delivery update for {project}: {invoice_status}."
        BusinessSmsTemplateId.MEETING_REMINDER ->
            "Hello {client}, reminder from {business}: meeting scheduled for {meeting_time}."
        BusinessSmsTemplateId.SUBSCRIPTION_REMINDER ->
            "Hello {client}, {business} subscription reminder: renewal on {subscription_date}. Amount: {amount}."
        BusinessSmsTemplateId.PAYMENT_RECEIPT ->
            "Hello {client}, payment received by {business}. Amount: {amount}. Thank you!"
        BusinessSmsTemplateId.LARGE_TRANSACTION ->
            "Hello {client}, {business} alert: large transaction {amount} on your account."
        BusinessSmsTemplateId.CUSTOM ->
            ""
    }
}

data class BusinessSmsTemplateContext(
    val businessName: String,
    val clientName: String = "Client",
    val amountLabel: String = "",
    val dueDateLabel: String = "",
    val invoiceStatusLabel: String = "",
    val projectName: String = "",
    val productName: String = "",
    val meetingTimeLabel: String = "",
    val subscriptionDateLabel: String = "",
) {
    companion object {
        fun forClient(businessName: String, client: BusinessClient): BusinessSmsTemplateContext =
            BusinessSmsTemplateContext(
                businessName = businessName,
                clientName = client.name,
            )

        fun forInvoice(
            businessName: String,
            client: BusinessClient,
            invoice: BusinessInvoice,
        ): BusinessSmsTemplateContext = BusinessSmsTemplateContext(
            businessName = businessName,
            clientName = client.name,
            amountLabel = Formatters.currencyFromCents(invoice.amountCents),
            dueDateLabel = Formatters.shortDate(invoice.dueDate),
            invoiceStatusLabel = invoice.status.name.lowercase().replace('_', ' '),
        )

        fun forProject(
            businessName: String,
            client: BusinessClient,
            project: BusinessProject,
        ): BusinessSmsTemplateContext = BusinessSmsTemplateContext(
            businessName = businessName,
            clientName = client.name,
            projectName = project.name,
            invoiceStatusLabel = project.status.name.lowercase().replace('_', ' '),
        )

        fun forTransaction(
            businessName: String,
            client: BusinessClient?,
            transaction: BusinessTransaction,
        ): BusinessSmsTemplateContext = BusinessSmsTemplateContext(
            businessName = businessName,
            clientName = client?.name ?: "Client",
            amountLabel = Formatters.currencyFromCents(transaction.amountCents),
        )
    }
}

fun InvoiceStatus.toSmsLabel(): String = name.lowercase().replace('_', ' ')
