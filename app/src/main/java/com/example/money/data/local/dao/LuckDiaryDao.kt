package com.example.money.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.money.data.local.entity.LuckDiaryEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LuckDiaryDao {

    @Query("SELECT * FROM luck_diary_entry ORDER BY entry_date DESC")
    fun observeAll(): Flow<List<LuckDiaryEntryEntity>>

    @Insert
    suspend fun insert(entry: LuckDiaryEntryEntity): Long

    @Delete
    suspend fun delete(entry: LuckDiaryEntryEntity)
}
