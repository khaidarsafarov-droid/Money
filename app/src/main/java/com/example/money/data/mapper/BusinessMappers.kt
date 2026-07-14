package com.example.money.data.mapper

import com.example.money.data.local.entity.BusinessAssetEntity
import com.example.money.data.local.entity.BusinessCategoryEntity
import com.example.money.data.local.entity.BusinessClientEntity
import com.example.money.data.local.entity.BusinessInvoiceEntity
import com.example.money.data.local.entity.BusinessProjectEntity
import com.example.money.data.local.entity.BusinessTransactionEntity
import com.example.money.domain.model.business.BusinessAsset
import com.example.money.domain.model.business.BusinessAssetType
import com.example.money.domain.model.business.BusinessCategory
import com.example.money.domain.model.business.BusinessClient
import com.example.money.domain.model.business.BusinessPaymentMethod
import com.example.money.domain.model.business.CategoryType
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
    tags = tags,
    createdAt = createdAt,
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
    tags = tags,
    createdAt = createdAt,
)

fun BusinessProjectEntity.toDomain(): BusinessProject = BusinessProject(
    id = id,
    syncId = syncId,
    clientId = clientId,
    name = name,
    status = runCatching { ProjectStatus.valueOf(status) }.getOrDefault(ProjectStatus.ACTIVE),
    budgetCents = budgetCents,
    note = note,
    startDate = startDate,
    endDate = endDate,
    createdAt = createdAt,
)

fun BusinessProject.toEntity(): BusinessProjectEntity = BusinessProjectEntity(
    id = id,
    syncId = syncId.ifBlank { com.example.money.data.sync.SyncIds.newId() },
    clientId = clientId,
    name = name,
    status = status.name,
    budgetCents = budgetCents,
    note = note,
    startDate = startDate,
    endDate = endDate,
    createdAt = createdAt,
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
    currency = currency,
    createdAt = createdAt,
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
    currency = currency,
    createdAt = createdAt,
)

fun BusinessTransactionEntity.toDomain(): BusinessTransaction = BusinessTransaction(
    id = id,
    syncId = syncId,
    type = runCatching { BusinessTransactionType.valueOf(type) }.getOrDefault(BusinessTransactionType.EXPENSE),
    amountCents = amountCents,
    category = category,
    categoryId = categoryId,
    ledgerType = runCatching { BusinessLedgerType.valueOf(ledgerType) }.getOrDefault(BusinessLedgerType.OPERATING),
    currency = currency,
    paymentMethod = runCatching { BusinessPaymentMethod.valueOf(paymentMethod) }.getOrDefault(BusinessPaymentMethod.OTHER),
    taxType = taxType,
    clientId = clientId,
    projectId = projectId,
    invoiceId = invoiceId,
    date = date,
    note = note,
    createdAt = createdAt,
)

fun BusinessTransaction.toEntity(): BusinessTransactionEntity = BusinessTransactionEntity(
    id = id,
    syncId = syncId.ifBlank { com.example.money.data.sync.SyncIds.newId() },
    type = type.name,
    amountCents = amountCents,
    category = category,
    categoryId = categoryId,
    ledgerType = ledgerType.name,
    currency = currency,
    paymentMethod = paymentMethod.name,
    taxType = taxType,
    clientId = clientId,
    projectId = projectId,
    invoiceId = invoiceId,
    date = date,
    note = note,
    createdAt = createdAt,
)

fun BusinessCategoryEntity.toDomain(): BusinessCategory = BusinessCategory(
    id = id,
    syncId = syncId,
    name = name,
    type = runCatching { CategoryType.valueOf(type) }.getOrDefault(CategoryType.EXPENSE),
    createdAt = createdAt,
)

fun BusinessCategory.toEntity(): BusinessCategoryEntity = BusinessCategoryEntity(
    id = id,
    syncId = syncId.ifBlank { com.example.money.data.sync.SyncIds.newId() },
    name = name,
    type = type.name,
    createdAt = createdAt,
)

fun BusinessAssetEntity.toDomain(): BusinessAsset = BusinessAsset(
    id = id,
    syncId = syncId,
    name = name,
    type = runCatching { BusinessAssetType.valueOf(type) }.getOrDefault(BusinessAssetType.OTHER),
    balanceCents = balanceCents,
    currency = currency,
    createdAt = createdAt,
)

fun BusinessAsset.toEntity(): BusinessAssetEntity = BusinessAssetEntity(
    id = id,
    syncId = syncId.ifBlank { com.example.money.data.sync.SyncIds.newId() },
    name = name,
    type = type.name,
    balanceCents = balanceCents,
    currency = currency,
    createdAt = createdAt,
)
