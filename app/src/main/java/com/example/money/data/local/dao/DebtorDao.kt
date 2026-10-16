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
    @Query("SELECT * FROM debtor WHERE mode = :mode ORDER BY date_due ASC")
    fun getAllDebtorsWithPayments(mode: String): Flow<List<DebtorWithPayments>>

    @Transaction
    @Query("SELECT * FROM debtor WHERE mode = :mode AND status != 'COMPLETED' ORDER BY date_due ASC")
    fun getActiveDebtorsWithPayments(mode: String): Flow<List<DebtorWithPayments>>

    @Transaction
    @Query("SELECT * FROM debtor WHERE mode = :mode AND contact_lookup_key = :lookupKey ORDER BY date_given DESC")
    fun getDebtorsByContactLookupKey(mode: String, lookupKey: String): Flow<List<DebtorWithPayments>>

    @Query("SELECT * FROM debtor WHERE id = :id")
    suspend fun getDebtorById(id: Long): DebtorEntity?

    @Query("SELECT * FROM debtor ORDER BY date_due ASC")
    suspend fun getAllDebtorsOnce(): List<DebtorEntity>

    @Query("SELECT * FROM debtor WHERE sync_id = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): DebtorEntity?

    @Transaction
    @Query(
        """
        SELECT * FROM debtor
        WHERE mode = :mode
        AND debt_type = :debtType
        AND (
            :filter = 'ALL'
            OR (:filter = 'ACTIVE' AND status != 'COMPLETED')
            OR (:filter = 'PAID' AND status = 'COMPLETED')
            OR (:filter = 'OVERDUE' AND status != 'COMPLETED' AND date_due < :overdueBefore)
        )
        AND (
            :searchPattern = '%'
            OR LOWER(name) LIKE LOWER(:searchPattern)
            OR LOWER(note) LIKE LOWER(:searchPattern)
            OR phone LIKE :searchPattern
            OR LOWER(email) LIKE LOWER(:searchPattern)
        )
        ORDER BY date_due ASC
        """,
    )
    fun observeFilteredWithPayments(
        mode: String,
        debtType: String,
        filter: String,
        searchPattern: String,
        overdueBefore: Long,
    ): Flow<List<DebtorWithPayments>>

    @Insert
    suspend fun insert(debtor: DebtorEntity): Long

    @Update
    suspend fun update(debtor: DebtorEntity)

    @Query(
        """
        UPDATE debtor
        SET calendar_event_id = :eventId, calendar_sync_failed = :failed
        WHERE id = :debtorId
        """,
    )
    suspend fun updateCalendarSyncState(debtorId: Long, eventId: String?, failed: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(debtor: DebtorEntity)

    @Delete
    suspend fun delete(debtor: DebtorEntity)

    @Query("DELETE FROM debtor")
    suspend fun clearAll()
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

    @Query("SELECT * FROM debt_payment WHERE mode = :mode AND debtor_id = :debtorId ORDER BY paid_at DESC")
    suspend fun getPaymentsForDebtor(mode: String, debtorId: Long): List<DebtPaymentEntity>

    @Query("DELETE FROM debt_payment")
    suspend fun clearAll()
}
