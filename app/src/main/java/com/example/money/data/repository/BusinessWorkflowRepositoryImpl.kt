package com.example.money.data.repository

import com.example.money.data.local.dao.WorkflowBillDao
import com.example.money.data.local.dao.WorkflowCategoryDao
import com.example.money.data.local.dao.WorkflowDebtDao
import com.example.money.data.local.dao.WorkflowParticipantDao
import com.example.money.data.local.dao.WorkflowProcessDao
import com.example.money.data.mapper.toDomain
import com.example.money.data.mapper.toEntity
import com.example.money.domain.model.business.workflow.WorkflowBill
import com.example.money.domain.model.business.workflow.WorkflowCategory
import com.example.money.domain.model.business.workflow.WorkflowDebt
import com.example.money.domain.model.business.workflow.WorkflowDebtStatus
import com.example.money.domain.model.business.workflow.WorkflowDebtWithParticipant
import com.example.money.domain.model.business.workflow.WorkflowParticipant
import com.example.money.domain.model.business.workflow.WorkflowParticipantDraft
import com.example.money.domain.model.business.workflow.WorkflowProcess
import com.example.money.domain.repository.BusinessWorkflowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class BusinessWorkflowRepositoryImpl(
    private val categoryDao: WorkflowCategoryDao,
    private val processDao: WorkflowProcessDao,
    private val billDao: WorkflowBillDao,
    private val participantDao: WorkflowParticipantDao,
    private val debtDao: WorkflowDebtDao,
) : BusinessWorkflowRepository {

    override fun observeCategories(): Flow<List<WorkflowCategory>> =
        categoryDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeProcesses(categoryId: Long): Flow<List<WorkflowProcess>> =
        processDao.observeByCategory(categoryId).map { rows -> rows.map { it.toDomain() } }

    override fun observeBills(processId: Long): Flow<List<WorkflowBill>> =
        billDao.observeByProcess(processId).map { rows -> rows.map { it.toDomain() } }

    override fun observeDebtsWithParticipants(billId: Long): Flow<List<WorkflowDebtWithParticipant>> =
        combine(
            debtDao.observeByBill(billId),
            participantDao.observeAll(),
        ) { debts, participants ->
            val byId = participants.associateBy { it.id }
            debts.mapNotNull { debt ->
                val participant = byId[debt.participantId] ?: return@mapNotNull null
                WorkflowDebtWithParticipant(debt.toDomain(), participant.toDomain())
            }
        }

    override fun observeParticipants(): Flow<List<WorkflowParticipant>> =
        participantDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getCategory(id: Long): WorkflowCategory? =
        categoryDao.getById(id)?.toDomain()

    override suspend fun getProcess(id: Long): WorkflowProcess? =
        processDao.getById(id)?.toDomain()

    override suspend fun getBill(id: Long): WorkflowBill? =
        billDao.getById(id)?.toDomain()

    override suspend fun addProcess(process: WorkflowProcess): Long =
        processDao.insert(process.toEntity())

    override suspend fun addBill(bill: WorkflowBill): Long =
        billDao.insert(bill.toEntity())

    override suspend fun createBillWithDebts(
        bill: WorkflowBill,
        participantDrafts: List<WorkflowParticipantDraft>,
    ): Long {
        val billId = billDao.insert(bill.toEntity())
        val debtEntities = participantDrafts.map { draft ->
            val participantId = resolveParticipantId(draft)
            WorkflowDebt(
                billId = billId,
                participantId = participantId,
                amountCents = draft.amountCents,
            ).toEntity()
        }
        debtEntities.forEach { debtDao.insert(it) }
        return billId
    }

    override suspend fun upsertParticipant(participant: WorkflowParticipant): Long {
        if (participant.id > 0L) {
            participantDao.update(participant.toEntity())
            return participant.id
        }
        if (!participant.contactLookupKey.isNullOrBlank()) {
            participantDao.findByLookupKey(participant.contactLookupKey)?.let { existing ->
                val merged = existing.copy(
                    name = participant.name.ifBlank { existing.name },
                    phone = participant.phone.ifBlank { existing.phone },
                    email = participant.email.ifBlank { existing.email },
                    contactUri = participant.contactUri ?: existing.contactUri,
                )
                participantDao.update(merged)
                return merged.id
            }
        }
        return participantDao.insert(participant.toEntity())
    }

    override suspend fun markDebtPaid(debtId: Long) {
        val debt = debtDao.getById(debtId) ?: return
        debtDao.update(
            debt.copy(
                paidAmountCents = debt.amountCents,
                status = WorkflowDebtStatus.PAID.name,
            ),
        )
    }

    override suspend fun deleteProcess(processId: Long) {
        processDao.deleteById(processId)
    }

    override suspend fun deleteBill(billId: Long) {
        billDao.deleteById(billId)
    }

    private suspend fun resolveParticipantId(draft: WorkflowParticipantDraft): Long {
        if (!draft.contactLookupKey.isNullOrBlank()) {
            participantDao.findByLookupKey(draft.contactLookupKey)?.id?.let { return it }
        }
        return participantDao.insert(
            WorkflowParticipant(
                name = draft.name,
                phone = draft.phone,
                email = draft.email,
                contactUri = draft.contactUri,
                contactLookupKey = draft.contactLookupKey,
            ).toEntity(),
        )
    }
}
