package com.example.money.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.money.data.local.entity.BusinessInvoiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessInvoiceDao {

    @Query("SELECT * FROM business_invoice ORDER BY due_date DESC")
    fun observeAll(): Flow<List<BusinessInvoiceEntity>>

    @Query("SELECT * FROM business_invoice WHERE client_id = :clientId ORDER BY due_date DESC")
    fun observeByClient(clientId: Long): Flow<List<BusinessInvoiceEntity>>

    @Query("SELECT * FROM business_invoice WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BusinessInvoiceEntity?

    @Query("SELECT * FROM business_invoice WHERE status IN ('SENT', 'OVERDUE')")
    suspend fun getOpenReceivables(): List<BusinessInvoiceEntity>

    @Query("SELECT * FROM business_invoice")
    suspend fun getAllOnce(): List<BusinessInvoiceEntity>

    @Insert
    suspend fun insert(invoice: BusinessInvoiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(invoice: BusinessInvoiceEntity)

    @Delete
    suspend fun delete(invoice: BusinessInvoiceEntity)
}
