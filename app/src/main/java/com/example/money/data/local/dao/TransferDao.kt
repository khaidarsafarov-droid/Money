package com.example.money.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.money.data.local.entity.TransferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {

    @Query("SELECT * FROM transfers ORDER BY date DESC")
    fun getAllTransfers(): Flow<List<TransferEntity>>

    @Query("SELECT COALESCE(SUM(amount_cents), 0) FROM transfers WHERE isConfirmed = 1 AND goalId IS NULL")
    fun observeDepositTotal(): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount_cents), 0) FROM transfers WHERE isConfirmed = 1 AND goalId IS NULL")
    suspend fun getDepositTotalOnce(): Long

    @Query("SELECT COALESCE(SUM(amount_cents), 0) FROM transfers WHERE isConfirmed = 1 AND goalId IS NOT NULL")
    fun observeGoalAllocatedTotal(): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount_cents), 0) FROM transfers WHERE isConfirmed = 1 AND goalId IS NOT NULL")
    suspend fun getGoalAllocatedTotalOnce(): Long

    @Query(
        """
        SELECT COALESCE(SUM(amount_cents), 0) FROM transfers
        WHERE isConfirmed = 1 AND goalId = :goalId
        """,
    )
    suspend fun getGoalTransferTotalOnce(goalId: Long): Long

    @Query(
        """
        SELECT goalId, COALESCE(SUM(amount_cents), 0) AS totalAmount
        FROM transfers
        WHERE isConfirmed = 1 AND goalId IS NOT NULL
        GROUP BY goalId
        """,
    )
    fun observeGoalTransferTotals(): Flow<List<GoalTransferTotal>>

    @Query("SELECT COUNT(*) FROM transfers WHERE isConfirmed = 0")
    fun observePendingCount(): Flow<Int>

    @Query("UPDATE transfers SET isConfirmed = 1 WHERE id = :id")
    suspend fun confirm(id: Long)

    @Query("SELECT * FROM transfers ORDER BY date DESC")
    suspend fun getAllOnce(): List<TransferEntity>

    @Query("SELECT * FROM transfers WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransferEntity?

    @Query("SELECT * FROM transfers WHERE sync_id = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): TransferEntity?

    @Query("SELECT * FROM transfers WHERE goalId = :goalId")
    suspend fun getByGoalId(goalId: Long): List<TransferEntity>

    @Query("SELECT * FROM transfers WHERE debt_id = :debtId")
    suspend fun getByDebtId(debtId: Long): List<TransferEntity>

    @Query("DELETE FROM transfers WHERE debt_id = :debtId")
    suspend fun deleteByDebtId(debtId: Long)

    @Insert
    suspend fun insert(transfer: TransferEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transfer: TransferEntity)

    @Delete
    suspend fun delete(transfer: TransferEntity)

    @Query("DELETE FROM transfers")
    suspend fun clearAll()
}
