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

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE isConfirmed = 1")
    fun observeConfirmedTotalAmount(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE isConfirmed = 1")
    suspend fun getConfirmedTotalOnce(): Double

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
}
