package com.example.money.data.repository

import com.example.money.data.local.dao.BusinessSmsLogDao
import com.example.money.data.mapper.toDomain
import com.example.money.data.mapper.toEntity
import com.example.money.domain.model.business.BusinessSmsLog
import com.example.money.domain.repository.BusinessSmsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BusinessSmsRepositoryImpl(
    private val smsLogDao: BusinessSmsLogDao,
) : BusinessSmsRepository {

    override fun observeSmsLogs(): Flow<List<BusinessSmsLog>> =
        smsLogDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun logSms(log: BusinessSmsLog): Long =
        smsLogDao.insert(log.toEntity())

    override suspend fun pruneOldLogs(olderThanMillis: Long) {
        smsLogDao.deleteOlderThan(olderThanMillis)
    }

    override suspend fun wasAutomationSentRecently(
        clientId: Long,
        templateId: String,
        withinMillis: Long,
    ): Boolean {
        val sinceMillis = System.currentTimeMillis() - withinMillis
        return smsLogDao.countRecentAutomation(clientId, templateId, sinceMillis) > 0
    }

    override suspend fun wasWorkflowCommunicationSentRecently(
        debtId: Long,
        channel: String,
        withinMillis: Long,
    ): Boolean {
        val sinceMillis = System.currentTimeMillis() - withinMillis
        return smsLogDao.countRecentWorkflowCommunication(debtId, channel, sinceMillis) > 0
    }
}
