package com.example.money.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.money.data.local.entity.BusinessTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessTransactionDao {

    @Query("SELECT * FROM business_transaction ORDER BY date DESC")
    fun observeAll(): Flow<List<BusinessTransactionEntity>>

    @Query("SELECT * FROM business_transaction WHERE date >= :fromMillis AND date < :toMillis ORDER BY date DESC")
    fun observeInRange(fromMillis: Long, toMillis: Long): Flow<List<BusinessTransactionEntity>>

    @Query("SELECT * FROM business_transaction WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BusinessTransactionEntity?

    @Query("SELECT * FROM business_transaction WHERE date >= :fromMillis AND date < :toMillis")
    suspend fun getInRangeOnce(fromMillis: Long, toMillis: Long): List<BusinessTransactionEntity>

    @Insert
    suspend fun insert(transaction: BusinessTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: BusinessTransactionEntity)

    @Delete
    suspend fun delete(transaction: BusinessTransactionEntity)
}
