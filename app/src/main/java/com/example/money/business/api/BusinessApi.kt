package com.example.money.business.api

import android.content.Context
import com.example.money.domain.model.business.BusinessCommunicationSendResult
import com.example.money.domain.model.business.workflow.WorkflowBill
import com.example.money.domain.model.business.workflow.WorkflowCategory
import com.example.money.domain.model.business.workflow.WorkflowDebtWithParticipant
import com.example.money.domain.model.business.workflow.WorkflowParticipantDraft
import com.example.money.domain.model.business.workflow.WorkflowProcess
import com.example.money.business.automation.BusinessAutomationRunResult
import com.example.money.business.automation.BusinessAutomationCenterUseCases
import com.example.money.domain.usecase.BusinessCommunicationUseCases
import com.example.money.domain.usecase.BusinessWorkflowUseCases
import com.example.money.messaging.MessagingChannel
import com.example.money.settings.BusinessBranchSettings
import kotlinx.coroutines.flow.Flow

/**
 * Unified Business API — maps logical REST routes to use cases.
 *
 * @see docs/business/API_REFERENCE.md
 */
class BusinessApi(
    private val workflowUseCases: BusinessWorkflowUseCases,
    private val communicationUseCases: BusinessCommunicationUseCases,
    private val automationCenter: BusinessAutomationCenterUseCases,
) {
    // /business/categories
    fun observeCategories(): Flow<List<WorkflowCategory>> = workflowUseCases.observeCategories()

    // /business/processes
    fun observeProcesses(categoryId: Long): Flow<List<WorkflowProcess>> =
        workflowUseCases.observeProcesses(categoryId)

    suspend fun createProcess(categoryId: Long, name: String, description: String): Long =
        workflowUseCases.createProcess(categoryId, name, description)

    // /business/bills
    fun observeBills(processId: Long): Flow<List<WorkflowBill>> =
        workflowUseCases.observeBills(processId)

    suspend fun createBillWithDebts(
        processId: Long,
        title: String,
        totalAmountCents: Long,
        description: String,
        periodLabel: String,
        participants: List<WorkflowParticipantDraft>,
    ): Long = workflowUseCases.createBillWithDebts(
        processId, title, totalAmountCents, description, periodLabel, participants,
    )

    // /business/debts
    fun observeDebtsWithParticipants(billId: Long): Flow<List<WorkflowDebtWithParticipant>> =
        workflowUseCases.observeDebtsWithParticipants(billId)

    suspend fun markDebtPaid(debtId: Long) = workflowUseCases.markDebtPaid(debtId)

    // /business/messages/ai-generate
    suspend fun aiGenerateMessage(
        billId: Long,
        debt: WorkflowDebtWithParticipant,
        businessName: String,
    ): String = communicationUseCases.aiGenerateWorkflowMessage(
        billId = billId,
        debt = debt,
        businessName = businessName,
    )

    // /business/messages/send-*
    suspend fun sendWhatsApp(
        context: Context,
        billId: Long,
        debts: List<WorkflowDebtWithParticipant>,
        messageBody: String,
        businessName: String,
    ): BusinessCommunicationSendResult = communicationUseCases.sendWorkflowReminders(
        context, billId, MessagingChannel.WHATSAPP, debts, messageBody,
        businessName = businessName,
    )

    suspend fun sendSms(
        context: Context,
        billId: Long,
        debts: List<WorkflowDebtWithParticipant>,
        messageBody: String,
        useDirectSms: Boolean,
        businessName: String,
    ): BusinessCommunicationSendResult = communicationUseCases.sendWorkflowReminders(
        context, billId, MessagingChannel.SMS, debts, messageBody, useDirectSms, businessName,
    )

    suspend fun sendPush(
        context: Context,
        billId: Long,
        debts: List<WorkflowDebtWithParticipant>,
        messageBody: String,
        businessName: String,
    ): BusinessCommunicationSendResult = communicationUseCases.sendWorkflowPushReminders(
        context, billId, debts, messageBody, businessName,
    )

    suspend fun sendBulk(
        context: Context,
        billId: Long,
        debts: List<WorkflowDebtWithParticipant>,
        messageBody: String,
        useDirectSms: Boolean,
        includePush: Boolean,
        businessName: String,
    ): BusinessCommunicationSendResult = communicationUseCases.sendWorkflowBulk(
        context = context,
        billId = billId,
        channels = listOf(
            MessagingChannel.WHATSAPP,
            MessagingChannel.SMS,
            MessagingChannel.TELEGRAM,
            MessagingChannel.EMAIL,
        ),
        selectedDebts = debts,
        messageBody = messageBody,
        useDirectSms = useDirectSms,
        includePush = includePush,
        businessName = businessName,
    )

    // /business/automation/run
    suspend fun runAutomation(
        context: Context,
        settings: BusinessBranchSettings,
    ): BusinessAutomationRunResult = automationCenter.runAll(context, settings)
}
