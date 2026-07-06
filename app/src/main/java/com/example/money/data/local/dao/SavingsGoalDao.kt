package com.example.money.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.money.data.local.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {

    @Query("SELECT * FROM savings_goal ORDER BY updated_at DESC")
    fun observeAllGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goal WHERE id = :id LIMIT 1")
    suspend fun getGoalById(id: Long): SavingsGoalEntity?

    @Query("SELECT * FROM savings_goal WHERE sync_id = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): SavingsGoalEntity?

    @Insert
    suspend fun insert(goal: SavingsGoalEntity): Long

    @Update
    suspend fun update(goal: SavingsGoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: SavingsGoalEntity)

    @Delete
    suspend fun delete(goal: SavingsGoalEntity)

    @Query("SELECT * FROM savings_goal ORDER BY updated_at DESC")
    suspend fun getAllGoalsOnce(): List<SavingsGoalEntity>
}
