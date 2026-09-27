package com.example.money.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.money.data.local.entity.LuckDiaryEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LuckDiaryDao {

    @Query("SELECT * FROM luck_diary_entry ORDER BY entry_date DESC")
    fun observeAll(): Flow<List<LuckDiaryEntryEntity>>

    @Query("SELECT * FROM luck_diary_entry ORDER BY entry_date DESC")
    suspend fun getAllOnce(): List<LuckDiaryEntryEntity>

    @Insert
    suspend fun insert(entry: LuckDiaryEntryEntity): Long

    @Update
    suspend fun update(entry: LuckDiaryEntryEntity)

    @Delete
    suspend fun delete(entry: LuckDiaryEntryEntity)

    @Query("DELETE FROM luck_diary_entry")
    suspend fun clearAll()
}
