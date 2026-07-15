package com.example.money.domain.usecase

import android.content.Context
import com.example.money.business.sms.BusinessSmsPermissions
import com.example.money.business.sms.BusinessSmsSender
import com.example.money.domain.model.business.BusinessCommunicationSendResult
import com.example.money.domain.model.business.BusinessSmsLog
import com.example.money.domain.model.business.BusinessSmsStatus
import com.example.money.domain.model.business.workflow.WorkflowDebtStatus
import com.example.money.domain.model.business.workflow.WorkflowDebtWithParticipant
import com.example.money.domain.repository.BusinessSmsRepository
import com.example.money.domain.repository.BusinessWorkflowRepository
import com.example.money.domain.util.ai.BusinessSmsTone
import com.example.money.messaging.MessagePayload
import com.example.money.messaging.MessagingChannel
import com.example.money.messaging.MessagingContact
import com.example.money.messaging.UniversalMessageSender
import com.example.money.notification.BusinessNotificationHelper
import com.example.money.settings.BusinessBranchSettings
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Unified Business communication API — workflow debts, invoices, clients.
 * Channels: WhatsApp, SMS, Push (local), Telegram, Email.
 */
class BusinessCommunicationUseCases(
    private val workflowRepository: BusinessWorkflowRepository,
    private val workflowUseCases: BusinessWorkflowUseCases,
    private val smsRepository: BusinessSmsRepository,
) {
    suspend fun aiGenerateWorkflowMessage(
        billId: Long,
        debt: WorkflowDebtWithParticipant,
        businessName: String,
        tone: BusinessSmsTone = BusinessSmsTone.FRIENDLY,
        languageHint: String = "ru",
    ): String {
        val base = workflowUseCases.aiGenerateReminderMessage(
            billId = billId,
            debt = debt,
            businessName = businessName,
            languageHint = languageHint,
        )
        return applyTone(base, tone)
    }

    suspend fun sendWorkflowReminders(
        context: Context,
        billId: Long,
        channel: MessagingChannel,
        selectedDebts: List<WorkflowDebtWithParticipant>,
        messageBody: String,
        useDirectSms: Boolean = false,
        businessName: String = "",
    ): BusinessCommunicationSendResult {
        var sent = 0
        var skipped = 0
        var failed = 0
        var opened = 0

        selectedDebts
            .filter { it.debt.status != WorkflowDebtStatus.PAID }
            .forEach { item ->
                val body = resolveBody(billId, item, messageBody, businessName)
                val status = when (channel) {
                    MessagingChannel.SMS -> sendSms(context, item, body, useDirectSms)
                    else -> sendMessagingChannel(context, item, channel, body)
                }
                if (status == BusinessSmsStatus.FAILED && item.participant.phone.isBlank() &&
                    channel != MessagingChannel.EMAIL
                ) {
                    skipped++
                } else {
                    logCommunication(item, body, channel, status)
                    when (status) {
                        BusinessSmsStatus.SENT -> sent++
                        BusinessSmsStatus.OPENED_APP -> opened++
                        BusinessSmsStatus.FAILED, BusinessSmsStatus.PENDING -> failed++
                    }
                }
            }

        return BusinessCommunicationSendResult(sent, skipped, failed, opened)
    }

    suspend fun sendWorkflowPushReminders(
        context: Context,
        billId: Long,
        selectedDebts: List<WorkflowDebtWithParticipant>,
        messageBody: String,
        businessName: String = "",
    ): BusinessCommunicationSendResult {
        var sent = 0
        var skipped = 0
        val bill = workflowRepository.getBill(billId)
        selectedDebts
            .filter { it.debt.status != WorkflowDebtStatus.PAID }
            .forEach { item ->
                val body = resolveBody(billId, item, messageBody, businessName)
                val shown = BusinessNotificationHelper.showWorkflowDebtReminder(
                    context = context,
                    debt = item,
                    billTitle = bill?.title.orEmpty(),
                    body = body,
                )
                val status = if (shown) BusinessSmsStatus.OPENED_APP else BusinessSmsStatus.FAILED
                logCommunication(item, body, MessagingChannel.SMS, status, channelLabel = "PUSH")
                if (shown) sent++ else skipped++
            }
        return BusinessCommunicationSendResult(sent, skipped, 0, sent)
    }

    suspend fun sendWorkflowBulk(
        context: Context,
        billId: Long,
        channels: List<MessagingChannel>,
        selectedDebts: List<WorkflowDebtWithParticipant>,
        messageBody: String,
        useDirectSms: Boolean,
        includePush: Boolean,
        businessName: String,
    ): BusinessCommunicationSendResult {
        var totalSent = 0
        var totalSkipped = 0
        var totalFailed = 0
        var totalOpened = 0
        channels.forEach { channel ->
            val partial = sendWorkflowReminders(
                context, billId, channel, selectedDebts, messageBody, useDirectSms, businessName,
            )
            totalSent += partial.sentCount
            totalSkipped += partial.skippedCount
            totalFailed += partial.failedCount
            totalOpened += partial.openedAppCount
        }
        if (includePush) {
            val push = sendWorkflowPushReminders(context, billId, selectedDebts, messageBody, businessName)
            totalSent += push.sentCount
            totalSkipped += push.skippedCount
            totalOpened += push.openedAppCount
        }
        return BusinessCommunicationSendResult(totalSent, totalSkipped, totalFailed, totalOpened)
    }

    suspend fun runWorkflowAutomation(
        context: Context,
        settings: BusinessBranchSettings,
    ): BusinessCommunicationSendResult {
        if (!settings.workflowDebtAutomationEnabled) {
            return BusinessCommunicationSendResult(0, 0, 0)
        }
        val channels = settings.enabledWorkflowAutomationChannels()
        if (channels.isEmpty() && !settings.workflowDebtPushEnabled) {
            return BusinessCommunicationSendResult(0, 0, 0)
        }

        var sent = 0
        var skipped = 0
        var failed = 0
        val businessName = settings.businessName

        val categories = workflowRepository.observeCategories().first()
        for (category in categories) {
            val processes = workflowRepository.observeProcesses(category.id).first()
            for (process in processes) {
                val bills = workflowRepository.observeBills(process.id).first()
                for (bill in bills) {
                    val debts = workflowRepository.observeDebtsWithParticipants(bill.id).first()
                        .filter { it.debt.status != WorkflowDebtStatus.PAID }
                    val pending = debts.filter { debt ->
                        !smsRepository.wasWorkflowCommunicationSentRecently(
                            debtId = debt.debt.id,
                            channel = "AUTOMATION",
                            withinMillis = AUTOMATION_DEDUP_MS,
                        )
                    }
                    if (pending.isEmpty()) continue
                    val body = workflowUseCases.aiGenerateReminderMessage(
                        billId = bill.id,
                        debt = pending.first(),
                        businessName = businessName,
                    )
                    val bulk = sendWorkflowBulk(
                        context = context,
                        billId = bill.id,
                        channels = channels,
                        selectedDebts = pending,
                        messageBody = body,
                        useDirectSms = settings.smsDirectSendEnabled,
                        includePush = settings.workflowDebtPushEnabled,
                        businessName = businessName,
                    )
                    sent += bulk.sentCount
                    skipped += bulk.skippedCount
                    failed += bulk.failedCount
                    pending.forEach { debt ->
                        smsRepository.logSms(
                            BusinessSmsLog(
                                recipientPhone = debt.participant.phone,
                                recipientName = debt.participant.name,
                                body = body,
                                templateId = "WORKFLOW_AUTOMATION",
                                channel = "AUTOMATION",
                                workflowDebtId = debt.debt.id,
                                status = BusinessSmsStatus.SENT,
                            ),
                        )
                    }
                }
            }
        }
        return BusinessCommunicationSendResult(sent, skipped, failed)
    }

    private suspend fun resolveBody(
        billId: Long,
        debt: WorkflowDebtWithParticipant,
        messageBody: String,
        businessName: String,
    ): String = if (messageBody.isBlank()) {
        workflowUseCases.buildReminderMessage(billId, debt, businessName)
    } else {
        messageBody.replace("{name}", debt.participant.name)
    }

    private fun sendSms(
        context: Context,
        item: WorkflowDebtWithParticipant,
        body: String,
        useDirectSms: Boolean,
    ): BusinessSmsStatus {
        val phone = item.participant.phone.trim()
        if (phone.isBlank()) return BusinessSmsStatus.FAILED
        return if (useDirectSms && BusinessSmsPermissions.canSendDirect(context)) {
            when (BusinessSmsSender.sendDirect(context, phone, body)) {
                BusinessSmsStatus.SENT -> BusinessSmsStatus.SENT
                BusinessSmsStatus.OPENED_APP -> BusinessSmsStatus.OPENED_APP
                BusinessSmsStatus.FAILED, BusinessSmsStatus.PENDING -> BusinessSmsStatus.FAILED
            }
        } else if (BusinessSmsSender.openSmsApp(context, phone, body)) {
            BusinessSmsStatus.OPENED_APP
        } else {
            BusinessSmsStatus.FAILED
        }
    }

    private fun sendMessagingChannel(
        context: Context,
        item: WorkflowDebtWithParticipant,
        channel: MessagingChannel,
        body: String,
    ): BusinessSmsStatus {
        val contact = MessagingContact(
            phone = item.participant.phone,
            email = item.participant.email,
            name = item.participant.name,
        )
        if (!UniversalMessageSender.canSend(channel, contact, context)) return BusinessSmsStatus.FAILED
        return when (
            UniversalMessageSender.send(context, channel, contact, MessagePayload(body = body))
        ) {
            com.example.money.messaging.MessageSendResult.SENT -> BusinessSmsStatus.SENT
            com.example.money.messaging.MessageSendResult.OPENED_APP -> BusinessSmsStatus.OPENED_APP
            com.example.money.messaging.MessageSendResult.FAILED -> BusinessSmsStatus.FAILED
        }
    }

    private suspend fun logCommunication(
        item: WorkflowDebtWithParticipant,
        body: String,
        channel: MessagingChannel,
        status: BusinessSmsStatus,
        channelLabel: String? = null,
    ) {
        smsRepository.logSms(
            BusinessSmsLog(
                recipientPhone = item.participant.phone,
                recipientName = item.participant.name,
                body = body,
                templateId = "WORKFLOW_DEBT",
                channel = channelLabel ?: channel.name,
                workflowDebtId = item.debt.id,
                status = status,
            ),
        )
    }

    private fun applyTone(message: String, tone: BusinessSmsTone): String = when (tone) {
        BusinessSmsTone.FORMAL -> message.replace("Привет", "Здравствуйте").replace("Hi", "Dear")
        BusinessSmsTone.FRIENDLY -> message
        BusinessSmsTone.URGENT -> "СРОЧНО: $message"
    }

    private companion object {
        val AUTOMATION_DEDUP_MS: Long = TimeUnit.HOURS.toMillis(24)
    }
}
