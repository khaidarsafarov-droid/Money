package com.example.money.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.money.data.local.entity.BusinessProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessProjectDao {

    @Query("SELECT * FROM business_project ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<BusinessProjectEntity>>

    @Query("SELECT * FROM business_project WHERE client_id = :clientId ORDER BY name COLLATE NOCASE ASC")
    fun observeByClient(clientId: Long): Flow<List<BusinessProjectEntity>>

    @Query("SELECT * FROM business_project WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BusinessProjectEntity?

    @Insert
    suspend fun insert(project: BusinessProjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: BusinessProjectEntity)

    @Delete
    suspend fun delete(project: BusinessProjectEntity)
}
