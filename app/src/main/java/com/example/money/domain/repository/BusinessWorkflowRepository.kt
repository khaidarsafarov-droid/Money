package com.example.money.domain.repository

import com.example.money.domain.model.business.workflow.WorkflowBill
import com.example.money.domain.model.business.workflow.WorkflowCategory
import com.example.money.domain.model.business.workflow.WorkflowDebt
import com.example.money.domain.model.business.workflow.WorkflowDebtWithParticipant
import com.example.money.domain.model.business.workflow.WorkflowParticipant
import com.example.money.domain.model.business.workflow.WorkflowParticipantDraft
import com.example.money.domain.model.business.workflow.WorkflowProcess
import kotlinx.coroutines.flow.Flow

interface BusinessWorkflowRepository {

    fun observeCategories(): Flow<List<WorkflowCategory>>

    fun observeProcesses(categoryId: Long): Flow<List<WorkflowProcess>>

    fun observeBills(processId: Long): Flow<List<WorkflowBill>>

    fun observeDebtsWithParticipants(billId: Long): Flow<List<WorkflowDebtWithParticipant>>

    fun observeParticipants(): Flow<List<WorkflowParticipant>>

    suspend fun getCategory(id: Long): WorkflowCategory?

    suspend fun getProcess(id: Long): WorkflowProcess?

    suspend fun getBill(id: Long): WorkflowBill?

    suspend fun addProcess(process: WorkflowProcess): Long

    suspend fun addBill(bill: WorkflowBill): Long

    suspend fun createBillWithDebts(
        bill: WorkflowBill,
        participantDrafts: List<WorkflowParticipantDraft>,
    ): Long

    suspend fun upsertParticipant(participant: WorkflowParticipant): Long

    suspend fun markDebtPaid(debtId: Long)

    suspend fun deleteProcess(processId: Long)

    suspend fun deleteBill(billId: Long)
}
