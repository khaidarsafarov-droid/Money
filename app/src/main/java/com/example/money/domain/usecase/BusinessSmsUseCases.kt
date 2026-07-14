package com.example.money.domain.usecase

import android.content.Context
import com.example.money.business.sms.BusinessSmsPermissions
import com.example.money.business.sms.BusinessSmsSender
import com.example.money.business.sms.BusinessSmsTemplateContext
import com.example.money.business.sms.BusinessSmsTemplates
import com.example.money.domain.model.business.BusinessClient
import com.example.money.domain.model.business.BusinessSmsLog
import com.example.money.domain.model.business.BusinessSmsRecipient
import com.example.money.domain.model.business.BusinessSmsSendRequest
import com.example.money.domain.model.business.BusinessSmsSendResult
import com.example.money.domain.model.business.BusinessSmsStatus
import com.example.money.domain.model.business.BusinessSmsTemplate
import com.example.money.domain.model.business.BusinessSmsTemplateId
import com.example.money.domain.model.business.InvoiceStatus
import com.example.money.domain.repository.BusinessRepository
import com.example.money.domain.repository.BusinessSmsRepository
import com.example.money.domain.util.ai.AiBusinessSmsService
import com.example.money.domain.util.ai.BusinessSmsTone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Offline Business SMS "API" — mirrors REST SMS endpoints.
 */
class BusinessSmsUseCases(
    private val context: Context,
    private val businessRepository: BusinessRepository,
    private val smsRepository: BusinessSmsRepository,
) {
    fun observeSmsHistory(): Flow<List<BusinessSmsLog>> = smsRepository.observeSmsLogs()

    fun listTemplates(): List<BusinessSmsTemplate> = BusinessSmsTemplates.all

    suspend fun renderTemplate(
        templateId: BusinessSmsTemplateId,
        clientId: Long?,
        businessName: String,
    ): String {
        val clients = businessRepository.observeClients().first()
        val client = clientId?.let { id -> clients.find { it.id == id } }
        val ctx = client?.let { BusinessSmsTemplateContext.forClient(businessName, it) }
            ?: BusinessSmsTemplateContext(businessName = businessName)
        return BusinessSmsTemplates.render(templateId, ctx)
    }

    suspend fun aiGenerateSms(
        templateId: BusinessSmsTemplateId,
        clientId: Long?,
        businessName: String,
        tone: BusinessSmsTone = BusinessSmsTone.FRIENDLY,
        languageHint: String = "en",
    ): String {
        val clients = businessRepository.observeClients().first()
        val client = clientId?.let { id -> clients.find { it.id == id } }
        val ctx = client?.let { BusinessSmsTemplateContext.forClient(businessName, it) }
            ?: BusinessSmsTemplateContext(businessName = businessName)
        return AiBusinessSmsService.generate(templateId, ctx, tone, languageHint)
    }

    suspend fun sendSms(request: BusinessSmsSendRequest): BusinessSmsSendResult {
        var sent = 0
        var failed = 0
        var openedApp = 0

        request.recipients.forEach { recipient ->
            val status = sendToRecipient(recipient, request.body, request.useDirectSend)
            when (status) {
                BusinessSmsStatus.SENT -> sent++
                BusinessSmsStatus.OPENED_APP -> openedApp++
                BusinessSmsStatus.FAILED, BusinessSmsStatus.PENDING -> failed++
            }
            smsRepository.logSms(
                BusinessSmsLog(
                    recipientPhone = recipient.phone,
                    recipientName = recipient.name,
                    clientId = recipient.clientId,
                    body = request.body,
                    templateId = request.templateId?.name,
                    status = status,
                ),
            )
        }
        return BusinessSmsSendResult(sentCount = sent, failedCount = failed, openedAppCount = openedApp)
    }

    suspend fun sendBulkToClients(
        clientIds: List<Long>,
        body: String,
        templateId: BusinessSmsTemplateId?,
        useDirectSend: Boolean,
    ): BusinessSmsSendResult {
        val clients = businessRepository.observeClients().first()
        val recipients = clientIds.mapNotNull { id ->
            clients.find { it.id == id }?.toSmsRecipient()
        }
        return sendSms(
            BusinessSmsSendRequest(
                recipients = recipients,
                body = body,
                templateId = templateId,
                useDirectSend = useDirectSend,
            ),
        )
    }

    suspend fun buildOverdueInvoiceSms(
        businessName: String,
        useDirectSend: Boolean,
    ): BusinessSmsSendResult {
        val clients = businessRepository.observeClients().first()
        val invoices = businessRepository.observeInvoices().first()
        val now = System.currentTimeMillis()
        val overdue = invoices.filter {
            it.status != InvoiceStatus.PAID && it.dueDate < now
        }
        var sent = 0
        var failed = 0
        var opened = 0
        overdue.forEach { invoice ->
            val client = clients.find { it.id == invoice.clientId } ?: return@forEach
            val recipient = client.toSmsRecipient() ?: return@forEach
            val body = BusinessSmsTemplates.render(
                BusinessSmsTemplateId.DEBT_REMINDER,
                BusinessSmsTemplateContext.forInvoice(businessName, client, invoice),
            )
            val partial = sendSms(
                BusinessSmsSendRequest(
                    recipients = listOf(recipient),
                    body = body,
                    templateId = BusinessSmsTemplateId.DEBT_REMINDER,
                    useDirectSend = useDirectSend,
                ),
            )
            sent += partial.sentCount
            failed += partial.failedCount
            opened += partial.openedAppCount
        }
        return BusinessSmsSendResult(sent, failed, opened)
    }

    private fun sendToRecipient(
        recipient: BusinessSmsRecipient,
        body: String,
        useDirectSend: Boolean,
    ): BusinessSmsStatus {
        return if (useDirectSend && BusinessSmsPermissions.canSendDirect(context)) {
            BusinessSmsSender.sendDirect(context, recipient.phone, body)
        } else if (BusinessSmsSender.openSmsApp(context, recipient.phone, body)) {
            BusinessSmsStatus.OPENED_APP
        } else {
            BusinessSmsStatus.FAILED
        }
    }
}

private fun BusinessClient.toSmsRecipient(): BusinessSmsRecipient? {
    val phone = phone.trim()
    if (phone.isBlank()) return null
    return BusinessSmsRecipient(phone = phone, name = name, clientId = id)
}
