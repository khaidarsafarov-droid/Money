package com.example.money.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.money.data.local.entity.BusinessClientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessClientDao {

    @Query("SELECT * FROM business_client ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<BusinessClientEntity>>

    @Query("SELECT * FROM business_client WHERE is_active = 1 ORDER BY name COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<BusinessClientEntity>>

    @Query("SELECT * FROM business_client WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BusinessClientEntity?

    @Insert
    suspend fun insert(client: BusinessClientEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(client: BusinessClientEntity)

    @Delete
    suspend fun delete(client: BusinessClientEntity)
}
