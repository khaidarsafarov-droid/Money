package com.example.money.ui.business.workflow

import com.example.money.domain.model.business.workflow.WorkflowDebtStatus
import com.example.money.domain.model.business.workflow.WorkflowDebtWithParticipant

/**
 * Pure selection rules for workflow debt reminders — unit-testable without ViewModel mocks.
 */
object BusinessWorkflowDebtSelection {

    fun unpaidDebtIds(debts: List<WorkflowDebtWithParticipant>): Set<Long> =
        debts
            .filter { it.debt.status != WorkflowDebtStatus.PAID }
            .map { it.debt.id }
            .toSet()

    fun resolveSelectedDebtIds(
        debts: List<WorkflowDebtWithParticipant>,
        currentSelection: Set<Long>,
    ): Set<Long> = when {
        currentSelection.isNotEmpty() -> currentSelection
        else -> unpaidDebtIds(debts)
    }

    fun debtsForSend(
        debts: List<WorkflowDebtWithParticipant>,
        selectedIds: Set<Long>,
    ): List<WorkflowDebtWithParticipant> {
        val explicit = debts.filter { it.debt.id in selectedIds }
        if (explicit.isNotEmpty()) return explicit
        return debts.filter { it.debt.status != WorkflowDebtStatus.PAID }
    }

    fun canSendReminders(debts: List<WorkflowDebtWithParticipant>): Boolean =
        unpaidDebtIds(debts).isNotEmpty()
}
