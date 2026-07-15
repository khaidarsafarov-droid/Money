package com.example.money.domain.repository

import com.example.money.domain.model.business.BusinessSmsLog
import kotlinx.coroutines.flow.Flow

interface BusinessSmsRepository {
    fun observeSmsLogs(): Flow<List<BusinessSmsLog>>
    suspend fun logSms(log: BusinessSmsLog): Long
    suspend fun pruneOldLogs(olderThanMillis: Long)
    suspend fun wasAutomationSentRecently(
        clientId: Long,
        templateId: String,
        withinMillis: Long,
    ): Boolean
}
