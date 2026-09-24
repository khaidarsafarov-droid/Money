package com.example.money.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.money.data.local.entity.GoalActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalActionDao {

    @Query("SELECT * FROM goal_action ORDER BY sort_order ASC, id ASC")
    fun observeAll(): Flow<List<GoalActionEntity>>

    @Query("SELECT * FROM goal_action WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): GoalActionEntity?

    @Insert
    suspend fun insert(action: GoalActionEntity): Long

    @Update
    suspend fun update(action: GoalActionEntity)

    @Query("DELETE FROM goal_action WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM goal_action WHERE goal_id = :goalId")
    suspend fun maxSortOrder(goalId: Long): Int
}
