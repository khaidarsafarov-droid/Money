package com.example.money.data.mapper

import com.example.money.data.local.entity.workflow.WorkflowBillEntity
import com.example.money.data.local.entity.workflow.WorkflowCategoryEntity
import com.example.money.data.local.entity.workflow.WorkflowDebtEntity
import com.example.money.data.local.entity.workflow.WorkflowParticipantEntity
import com.example.money.data.local.entity.workflow.WorkflowProcessEntity
import com.example.money.domain.model.business.workflow.WorkflowBill
import com.example.money.domain.model.business.workflow.WorkflowCategory
import com.example.money.domain.model.business.workflow.WorkflowCategoryType
import com.example.money.domain.model.business.workflow.WorkflowDebt
import com.example.money.domain.model.business.workflow.WorkflowDebtStatus
import com.example.money.domain.model.business.workflow.WorkflowParticipant
import com.example.money.domain.model.business.workflow.WorkflowProcess

fun WorkflowCategoryEntity.toDomain(): WorkflowCategory = WorkflowCategory(
    id = id,
    name = name,
    type = runCatching { WorkflowCategoryType.valueOf(type) }.getOrDefault(WorkflowCategoryType.BUSINESS),
    sortOrder = sortOrder,
)

fun WorkflowCategory.toEntity(): WorkflowCategoryEntity = WorkflowCategoryEntity(
    id = id,
    name = name,
    type = type.name,
    sortOrder = sortOrder,
)

fun WorkflowProcessEntity.toDomain(): WorkflowProcess = WorkflowProcess(
    id = id,
    categoryId = categoryId,
    name = name,
    description = description,
    createdAt = createdAt,
)

fun WorkflowProcess.toEntity(): WorkflowProcessEntity = WorkflowProcessEntity(
    id = id,
    categoryId = categoryId,
    name = name,
    description = description,
    createdAt = createdAt,
)

fun WorkflowBillEntity.toDomain(): WorkflowBill = WorkflowBill(
    id = id,
    processId = processId,
    title = title,
    totalAmountCents = totalAmountCents,
    description = description,
    periodLabel = periodLabel,
    createdAt = createdAt,
)

fun WorkflowBill.toEntity(): WorkflowBillEntity = WorkflowBillEntity(
    id = id,
    processId = processId,
    title = title,
    totalAmountCents = totalAmountCents,
    description = description,
    periodLabel = periodLabel,
    createdAt = createdAt,
)

fun WorkflowParticipantEntity.toDomain(): WorkflowParticipant = WorkflowParticipant(
    id = id,
    name = name,
    phone = phone,
    email = email,
    contactUri = contactUri,
    contactLookupKey = contactLookupKey,
    createdAt = createdAt,
)

fun WorkflowParticipant.toEntity(): WorkflowParticipantEntity = WorkflowParticipantEntity(
    id = id,
    name = name,
    phone = phone,
    email = email,
    contactUri = contactUri,
    contactLookupKey = contactLookupKey,
    createdAt = createdAt,
)

fun WorkflowDebtEntity.toDomain(): WorkflowDebt = WorkflowDebt(
    id = id,
    billId = billId,
    participantId = participantId,
    amountCents = amountCents,
    paidAmountCents = paidAmountCents,
    status = runCatching { WorkflowDebtStatus.valueOf(status) }.getOrDefault(WorkflowDebtStatus.OPEN),
    createdAt = createdAt,
)

fun WorkflowDebt.toEntity(): WorkflowDebtEntity = WorkflowDebtEntity(
    id = id,
    billId = billId,
    participantId = participantId,
    amountCents = amountCents,
    paidAmountCents = paidAmountCents,
    status = status.name,
    createdAt = createdAt,
)
