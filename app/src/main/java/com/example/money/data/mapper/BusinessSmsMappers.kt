package com.example.money.data.mapper

import com.example.money.data.local.entity.BusinessSmsLogEntity
import com.example.money.domain.model.business.BusinessSmsLog
import com.example.money.domain.model.business.BusinessSmsStatus

fun BusinessSmsLogEntity.toDomain(): BusinessSmsLog = BusinessSmsLog(
    id = id,
    recipientPhone = recipientPhone,
    recipientName = recipientName,
    clientId = clientId,
    body = body,
    templateId = templateId,
    channel = channel,
    workflowDebtId = workflowDebtId,
    status = runCatching { BusinessSmsStatus.valueOf(status) }.getOrDefault(BusinessSmsStatus.PENDING),
    createdAt = createdAt,
)

fun BusinessSmsLog.toEntity(): BusinessSmsLogEntity = BusinessSmsLogEntity(
    id = id,
    recipientPhone = recipientPhone,
    recipientName = recipientName,
    clientId = clientId,
    body = body,
    templateId = templateId,
    channel = channel,
    workflowDebtId = workflowDebtId,
    status = status.name,
    createdAt = createdAt,
)
