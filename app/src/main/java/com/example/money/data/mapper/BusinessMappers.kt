package com.example.money.data.mapper

import com.example.money.data.local.entity.BusinessClientEntity
import com.example.money.data.local.entity.BusinessInvoiceEntity
import com.example.money.data.local.entity.BusinessProjectEntity
import com.example.money.data.local.entity.BusinessTransactionEntity
import com.example.money.domain.model.business.BusinessClient
import com.example.money.domain.model.business.BusinessInvoice
import com.example.money.domain.model.business.BusinessLedgerType
import com.example.money.domain.model.business.BusinessProject
import com.example.money.domain.model.business.BusinessTransaction
import com.example.money.domain.model.business.BusinessTransactionType
import com.example.money.domain.model.business.InvoiceStatus
import com.example.money.domain.model.business.ProjectStatus

fun BusinessClientEntity.toDomain(): BusinessClient = BusinessClient(
    id = id,
    syncId = syncId,
    name = name,
    contactInfo = contactInfo,
    email = email,
    phone = phone,
    isActive = isActive,
    note = note,
)

fun BusinessClient.toEntity(): BusinessClientEntity = BusinessClientEntity(
    id = id,
    syncId = syncId.ifBlank { com.example.money.data.sync.SyncIds.newId() },
    name = name,
    contactInfo = contactInfo,
    email = email,
    phone = phone,
    isActive = isActive,
    note = note,
)

fun BusinessProjectEntity.toDomain(): BusinessProject = BusinessProject(
    id = id,
    syncId = syncId,
    clientId = clientId,
    name = name,
    status = runCatching { ProjectStatus.valueOf(status) }.getOrDefault(ProjectStatus.ACTIVE),
    budgetCents = budgetCents,
    note = note,
)

fun BusinessProject.toEntity(): BusinessProjectEntity = BusinessProjectEntity(
    id = id,
    syncId = syncId.ifBlank { com.example.money.data.sync.SyncIds.newId() },
    clientId = clientId,
    name = name,
    status = status.name,
    budgetCents = budgetCents,
    note = note,
)

fun BusinessInvoiceEntity.toDomain(): BusinessInvoice = BusinessInvoice(
    id = id,
    syncId = syncId,
    clientId = clientId,
    projectId = projectId,
    issueDate = issueDate,
    dueDate = dueDate,
    amountCents = amountCents,
    status = runCatching { InvoiceStatus.valueOf(status) }.getOrDefault(InvoiceStatus.DRAFT),
    note = note,
)

fun BusinessInvoice.toEntity(): BusinessInvoiceEntity = BusinessInvoiceEntity(
    id = id,
    syncId = syncId.ifBlank { com.example.money.data.sync.SyncIds.newId() },
    clientId = clientId,
    projectId = projectId,
    issueDate = issueDate,
    dueDate = dueDate,
    amountCents = amountCents,
    status = status.name,
    note = note,
)

fun BusinessTransactionEntity.toDomain(): BusinessTransaction = BusinessTransaction(
    id = id,
    syncId = syncId,
    type = runCatching { BusinessTransactionType.valueOf(type) }.getOrDefault(BusinessTransactionType.EXPENSE),
    amountCents = amountCents,
    category = category,
    ledgerType = runCatching { BusinessLedgerType.valueOf(ledgerType) }.getOrDefault(BusinessLedgerType.OPERATING),
    clientId = clientId,
    projectId = projectId,
    invoiceId = invoiceId,
    date = date,
    note = note,
)

fun BusinessTransaction.toEntity(): BusinessTransactionEntity = BusinessTransactionEntity(
    id = id,
    syncId = syncId.ifBlank { com.example.money.data.sync.SyncIds.newId() },
    type = type.name,
    amountCents = amountCents,
    category = category,
    ledgerType = ledgerType.name,
    clientId = clientId,
    projectId = projectId,
    invoiceId = invoiceId,
    date = date,
    note = note,
)
