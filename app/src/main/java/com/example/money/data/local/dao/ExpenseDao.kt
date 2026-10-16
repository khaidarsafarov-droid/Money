package com.example.money.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.money.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT COALESCE(SUM(amount_cents), 0) FROM expenses WHERE isConfirmed = 1 AND goalId IS NULL")
    fun observeConfirmedTotalAmount(): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount_cents), 0) FROM expenses WHERE isConfirmed = 1 AND goalId IS NULL")
    suspend fun getConfirmedTotalOnce(): Long

    @Query(
        """
        SELECT goalId, COALESCE(SUM(amount_cents), 0) AS totalAmount
        FROM expenses
        WHERE isConfirmed = 1 AND goalId IS NOT NULL
        GROUP BY goalId
        """,
    )
    fun observeGoalExpenseTotals(): Flow<List<GoalTransferTotal>>

    @Query(
        """
        SELECT COALESCE(SUM(amount_cents), 0) FROM expenses
        WHERE isConfirmed = 1 AND goalId = :goalId
        """,
    )
    suspend fun getGoalExpenseTotalOnce(goalId: Long): Long

    @Query("SELECT COUNT(*) FROM expenses WHERE isConfirmed = 0")
    fun observePendingCount(): Flow<Int>

    @Query("UPDATE expenses SET isConfirmed = 1 WHERE id = :id")
    suspend fun confirm(id: Long)

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    suspend fun getAllOnce(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE sync_id = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE goalId = :goalId")
    suspend fun getByGoalId(goalId: Long): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE debt_id = :debtId")
    suspend fun getByDebtId(debtId: Long): List<ExpenseEntity>

    @Query("DELETE FROM expenses WHERE debt_id = :debtId")
    suspend fun deleteByDebtId(debtId: Long)

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses")
    suspend fun clearAll()
}
