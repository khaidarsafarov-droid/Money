package com.example.money.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.money.data.local.entity.DebtAdvanceEntity

@Dao
interface DebtAdvanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(advance: DebtAdvanceEntity)

    @Insert
    suspend fun insert(advance: DebtAdvanceEntity): Long

    @Update
    suspend fun update(advance: DebtAdvanceEntity)

    @Query("DELETE FROM debt_advance WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM debt_advance WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DebtAdvanceEntity?

    @Query("SELECT * FROM debt_advance WHERE sync_id = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): DebtAdvanceEntity?

    @Query("SELECT * FROM debt_advance ORDER BY given_at ASC")
    suspend fun getAllOnce(): List<DebtAdvanceEntity>

    @Query("SELECT * FROM debt_advance WHERE debtor_id = :debtorId ORDER BY given_at ASC, id ASC")
    suspend fun getForDebtor(debtorId: Long): List<DebtAdvanceEntity>

    @Query("DELETE FROM debt_advance")
    suspend fun clearAll()
}
