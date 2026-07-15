package com.example.money.ui.business.workflow

import com.example.money.domain.model.business.workflow.WorkflowParticipantDraft

data class ParticipantDraftRow(
    val name: String,
    val phone: String,
    val email: String = "",
    val contactUri: String? = null,
    val contactLookupKey: String? = null,
    val amountInput: String = "",
    val selected: Boolean = true,
)

data class CreateBillDraft(
    val processId: Long,
    val title: String = "",
    val totalInput: String = "",
    val description: String = "",
    val periodLabel: String = "",
    val participants: List<ParticipantDraftRow> = emptyList(),
) {
    fun toParticipantDrafts(): List<WorkflowParticipantDraft> = participants
        .filter { it.selected }
        .mapNotNull { row ->
            val cents = row.amountInput.replace(',', '.').toDoubleOrNull()?.let {
                com.example.money.domain.util.MoneyMath.toCents(it)
            } ?: 0L
            if (cents <= 0) return@mapNotNull null
            WorkflowParticipantDraft(
                name = row.name,
                phone = row.phone,
                email = row.email,
                contactUri = row.contactUri,
                contactLookupKey = row.contactLookupKey,
                amountCents = cents,
            )
        }
}

enum class WorkflowDebtFilter {
    ALL,
    UNPAID,
    PAID,
}
