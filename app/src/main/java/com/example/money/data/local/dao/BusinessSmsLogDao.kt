package com.example.money.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.money.data.local.entity.BusinessSmsLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessSmsLogDao {

    @Query("SELECT * FROM business_sms_log ORDER BY created_at DESC")
    fun observeAll(): Flow<List<BusinessSmsLogEntity>>

    @Query("SELECT * FROM business_sms_log ORDER BY created_at DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<BusinessSmsLogEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM business_sms_log
        WHERE client_id = :clientId
        AND template_id = :templateId
        AND created_at >= :sinceMillis
        AND status IN ('SENT', 'OPENED_APP')
        """,
    )
    suspend fun countRecentAutomation(clientId: Long, templateId: String, sinceMillis: Long): Int

    @Query(
        """
        SELECT COUNT(*) FROM business_sms_log
        WHERE workflow_debt_id = :debtId
        AND channel = :channel
        AND created_at >= :sinceMillis
        AND status IN ('SENT', 'OPENED_APP')
        """,
    )
    suspend fun countRecentWorkflowCommunication(
        debtId: Long,
        channel: String,
        sinceMillis: Long,
    ): Int

    @Insert
    suspend fun insert(log: BusinessSmsLogEntity): Long

    @Query("DELETE FROM business_sms_log WHERE created_at < :beforeMillis")
    suspend fun deleteOlderThan(beforeMillis: Long)
}
