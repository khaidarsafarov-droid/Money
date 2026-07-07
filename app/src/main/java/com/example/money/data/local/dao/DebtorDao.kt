package com.example.money.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.money.data.local.entity.DebtPaymentEntity
import com.example.money.data.local.entity.DebtorEntity
import com.example.money.data.local.entity.DebtorWithPayments
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtorDao {

    @Transaction
    @Query("SELECT * FROM debtor ORDER BY date_due ASC")
    fun getAllDebtorsWithPayments(): Flow<List<DebtorWithPayments>>

    @Transaction
    @Query("SELECT * FROM debtor WHERE status != 'COMPLETED' ORDER BY date_due ASC")
    fun getActiveDebtorsWithPayments(): Flow<List<DebtorWithPayments>>

    @Transaction
    @Query("SELECT * FROM debtor WHERE contact_lookup_key = :lookupKey ORDER BY date_given DESC")
    fun getDebtorsByContactLookupKey(lookupKey: String): Flow<List<DebtorWithPayments>>

    @Query("SELECT * FROM debtor WHERE id = :id")
    suspend fun getDebtorById(id: Long): DebtorEntity?

    @Query("SELECT * FROM debtor ORDER BY date_due ASC")
    suspend fun getAllDebtorsOnce(): List<DebtorEntity>

    @Query("SELECT * FROM debtor WHERE sync_id = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): DebtorEntity?

    @Insert
    suspend fun insert(debtor: DebtorEntity): Long

    @Update
    suspend fun update(debtor: DebtorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(debtor: DebtorEntity)

    @Delete
    suspend fun delete(debtor: DebtorEntity)
}

@Dao
interface DebtPaymentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(payment: DebtPaymentEntity)

    @Insert
    suspend fun insert(payment: DebtPaymentEntity): Long

    @Query("DELETE FROM debt_payment WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM debt_payment WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DebtPaymentEntity?

    @Query("SELECT * FROM debt_payment WHERE sync_id = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): DebtPaymentEntity?

    @Query("SELECT * FROM debt_payment ORDER BY paid_at DESC")
    suspend fun getAllPaymentsOnce(): List<DebtPaymentEntity>

    @Query("SELECT * FROM debt_payment WHERE debtor_id = :debtorId ORDER BY paid_at DESC")
    suspend fun getPaymentsForDebtor(debtorId: Long): List<DebtPaymentEntity>
}
