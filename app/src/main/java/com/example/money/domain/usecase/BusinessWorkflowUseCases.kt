package com.example.money.domain.usecase

import android.content.Context
import com.example.money.domain.model.business.workflow.WorkflowBill
import com.example.money.domain.model.business.workflow.WorkflowDebtWithParticipant
import com.example.money.domain.model.business.workflow.WorkflowParticipantDraft
import com.example.money.domain.model.business.workflow.WorkflowProcess
import com.example.money.domain.model.business.workflow.WorkflowReminderContext
import com.example.money.domain.model.business.workflow.WorkflowWhatsAppSendResult
import com.example.money.domain.repository.BusinessWorkflowRepository
import com.example.money.domain.util.ai.AiWorkflowMessageService
import com.example.money.messaging.MessagePayload
import com.example.money.messaging.MessagingChannel
import com.example.money.messaging.MessagingContact
import com.example.money.messaging.UniversalMessageSender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class BusinessWorkflowUseCases(
    private val repository: BusinessWorkflowRepository,
) {
    fun observeCategories() = repository.observeCategories()

    fun observeProcesses(categoryId: Long) = repository.observeProcesses(categoryId)

    fun observeBills(processId: Long) = repository.observeBills(processId)

    fun observeDebtsWithParticipants(billId: Long): Flow<List<WorkflowDebtWithParticipant>> =
        repository.observeDebtsWithParticipants(billId)

    suspend fun createProcess(categoryId: Long, name: String, description: String): Long =
        repository.addProcess(
            WorkflowProcess(
                categoryId = categoryId,
                name = name.trim(),
                description = description.trim(),
            ),
        )

    suspend fun createBillWithDebts(
        processId: Long,
        title: String,
        totalAmountCents: Long,
        description: String,
        periodLabel: String,
        participants: List<WorkflowParticipantDraft>,
    ): Long = repository.createBillWithDebts(
        bill = WorkflowBill(
            processId = processId,
            title = title.trim(),
            totalAmountCents = totalAmountCents,
            description = description.trim(),
            periodLabel = periodLabel.trim(),
        ),
        participantDrafts = participants.filter { it.amountCents > 0 },
    )

    fun splitEqually(totalAmountCents: Long, participantCount: Int): List<Long> {
        if (participantCount <= 0) return emptyList()
        val base = totalAmountCents / participantCount
        val remainder = (totalAmountCents % participantCount).toInt()
        return List(participantCount) { index ->
            base + if (index < remainder) 1L else 0L
        }
    }

    suspend fun buildReminderMessage(
        billId: Long,
        debt: WorkflowDebtWithParticipant,
        businessName: String,
        customBody: String? = null,
    ): String {
        if (!customBody.isNullOrBlank()) return customBody
        val bill = repository.getBill(billId) ?: return ""
        val process = repository.getProcess(bill.processId) ?: return ""
        return AiWorkflowMessageService.generate(
            WorkflowReminderContext(
                processName = process.name,
                billTitle = bill.title,
                billDescription = bill.description,
                periodLabel = bill.periodLabel,
                participantName = debt.participant.name,
                amountCents = debt.debt.remainingAmountCents,
                totalAmountCents = bill.totalAmountCents,
                businessName = businessName,
            ),
        )
    }

    suspend fun aiGenerateReminderMessage(
        billId: Long,
        debt: WorkflowDebtWithParticipant,
        businessName: String,
        languageHint: String = "ru",
    ): String {
        val bill = repository.getBill(billId) ?: return ""
        val process = repository.getProcess(bill.processId) ?: return ""
        return AiWorkflowMessageService.generate(
            WorkflowReminderContext(
                processName = process.name,
                billTitle = bill.title,
                billDescription = bill.description,
                periodLabel = bill.periodLabel,
                participantName = debt.participant.name,
                amountCents = debt.debt.remainingAmountCents,
                totalAmountCents = bill.totalAmountCents,
                businessName = businessName,
            ),
            languageHint = languageHint,
        )
    }

    suspend fun sendWhatsAppReminders(
        context: Context,
        billId: Long,
        selectedDebts: List<WorkflowDebtWithParticipant>,
        messageBody: String,
    ): WorkflowWhatsAppSendResult {
        var sent = 0
        var skipped = 0
        var failed = 0
        selectedDebts.forEach { item ->
            val phone = item.participant.phone.trim()
            if (phone.isBlank()) {
                skipped++
                return@forEach
            }
            val contact = MessagingContact(
                phone = phone,
                email = item.participant.email,
                name = item.participant.name,
            )
            if (!UniversalMessageSender.canSend(MessagingChannel.WHATSAPP, contact, context)) {
                skipped++
                return@forEach
            }
            val body = if (messageBody.isBlank()) {
                buildReminderMessage(billId, item, businessName = "")
            } else {
                personalizeMessage(messageBody, item.participant.name)
            }
            val result = UniversalMessageSender.send(
                context,
                MessagingChannel.WHATSAPP,
                contact,
                MessagePayload(body = body),
            )
            when (result) {
                com.example.money.messaging.MessageSendResult.OPENED_APP,
                com.example.money.messaging.MessageSendResult.SENT,
                -> sent++
                com.example.money.messaging.MessageSendResult.FAILED -> failed++
            }
        }
        return WorkflowWhatsAppSendResult(sent, skipped, failed)
    }

    suspend fun markDebtPaid(debtId: Long) = repository.markDebtPaid(debtId)

    suspend fun getProcess(id: Long) = repository.getProcess(id)

    suspend fun getBill(id: Long) = repository.getBill(id)

    suspend fun getCategory(id: Long) = repository.getCategory(id)

    private fun personalizeMessage(template: String, participantName: String): String =
        template.replace("{name}", participantName)
}
