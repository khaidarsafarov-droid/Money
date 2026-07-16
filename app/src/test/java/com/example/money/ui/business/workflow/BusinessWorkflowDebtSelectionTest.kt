package com.example.money.ui.business.workflow

import com.example.money.domain.model.business.workflow.WorkflowDebt
import com.example.money.domain.model.business.workflow.WorkflowDebtStatus
import com.example.money.domain.model.business.workflow.WorkflowDebtWithParticipant
import com.example.money.domain.model.business.workflow.WorkflowParticipant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessWorkflowDebtSelectionTest {

    private fun debt(
        id: Long,
        status: WorkflowDebtStatus,
        name: String = "P$id",
    ): WorkflowDebtWithParticipant = WorkflowDebtWithParticipant(
        debt = WorkflowDebt(id = id, billId = 1, participantId = id, amountCents = 100, status = status),
        participant = WorkflowParticipant(id = id, name = name, phone = "+7900$id"),
    )

    @Test
    fun `unpaidDebtIds excludes paid debts`() {
        val debts = listOf(
            debt(1, WorkflowDebtStatus.OPEN),
            debt(2, WorkflowDebtStatus.PAID),
            debt(3, WorkflowDebtStatus.PARTIAL),
        )
        assertEquals(setOf(1L, 3L), BusinessWorkflowDebtSelection.unpaidDebtIds(debts))
    }

    @Test
    fun `resolveSelectedDebtIds keeps explicit selection`() {
        val debts = listOf(
            debt(1, WorkflowDebtStatus.OPEN),
            debt(2, WorkflowDebtStatus.OPEN),
        )
        assertEquals(setOf(2L), BusinessWorkflowDebtSelection.resolveSelectedDebtIds(debts, setOf(2L)))
    }

    @Test
    fun `resolveSelectedDebtIds defaults to unpaid when empty`() {
        val debts = listOf(
            debt(1, WorkflowDebtStatus.PAID),
            debt(2, WorkflowDebtStatus.OPEN),
        )
        assertEquals(setOf(2L), BusinessWorkflowDebtSelection.resolveSelectedDebtIds(debts, emptySet()))
    }

    @Test
    fun `debtsForSend falls back to unpaid when nothing selected`() {
        val debts = listOf(
            debt(1, WorkflowDebtStatus.PAID),
            debt(2, WorkflowDebtStatus.OPEN),
        )
        val result = BusinessWorkflowDebtSelection.debtsForSend(debts, emptySet())
        assertEquals(1, result.size)
        assertEquals(2L, result.first().debt.id)
    }

    @Test
    fun `debtsForSend uses explicit selection when present`() {
        val debts = listOf(
            debt(1, WorkflowDebtStatus.OPEN),
            debt(2, WorkflowDebtStatus.OPEN),
        )
        val result = BusinessWorkflowDebtSelection.debtsForSend(debts, setOf(1L))
        assertEquals(1, result.size)
        assertEquals(1L, result.first().debt.id)
    }

    @Test
    fun `canSendReminders is false when all paid`() {
        val debts = listOf(debt(1, WorkflowDebtStatus.PAID))
        assertFalse(BusinessWorkflowDebtSelection.canSendReminders(debts))
    }

    @Test
    fun `canSendReminders is true when unpaid exists`() {
        val debts = listOf(debt(1, WorkflowDebtStatus.OPEN))
        assertTrue(BusinessWorkflowDebtSelection.canSendReminders(debts))
    }
}
