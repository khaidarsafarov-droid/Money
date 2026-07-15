package com.example.money.domain.model.business.workflow

enum class WorkflowCategoryType {
    BUSINESS,
    TRADE,
    WORK_PROCESS,
    FAMILY,
    SHARED,
    PROJECTS,
    SERVICES,
    SUBSCRIPTIONS,
}

enum class WorkflowDebtStatus {
    OPEN,
    PARTIAL,
    PAID,
}

data class WorkflowCategory(
    val id: Long = 0,
    val name: String,
    val type: WorkflowCategoryType,
    val sortOrder: Int = 0,
)

data class WorkflowProcess(
    val id: Long = 0,
    val categoryId: Long,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

data class WorkflowBill(
    val id: Long = 0,
    val processId: Long,
    val title: String,
    val totalAmountCents: Long,
    val description: String = "",
    val periodLabel: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

data class WorkflowParticipant(
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val contactUri: String? = null,
    val contactLookupKey: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

data class WorkflowDebt(
    val id: Long = 0,
    val billId: Long,
    val participantId: Long,
    val amountCents: Long,
    val paidAmountCents: Long = 0,
    val status: WorkflowDebtStatus = WorkflowDebtStatus.OPEN,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val remainingAmountCents: Long
        get() = (amountCents - paidAmountCents).coerceAtLeast(0)
}

data class WorkflowDebtWithParticipant(
    val debt: WorkflowDebt,
    val participant: WorkflowParticipant,
)

data class WorkflowParticipantDraft(
    val name: String,
    val phone: String,
    val email: String = "",
    val contactUri: String? = null,
    val contactLookupKey: String? = null,
    val amountCents: Long,
)

data class WorkflowReminderContext(
    val processName: String,
    val billTitle: String,
    val billDescription: String,
    val periodLabel: String,
    val participantName: String,
    val amountCents: Long,
    val totalAmountCents: Long,
    val businessName: String,
)

data class WorkflowWhatsAppSendResult(
    val sentCount: Int,
    val skippedCount: Int,
    val failedCount: Int,
)
