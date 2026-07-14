package com.example.money.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.money.data.local.entity.BusinessCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessCategoryDao {

    @Query("SELECT * FROM business_category ORDER BY type ASC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<BusinessCategoryEntity>>

    @Query("SELECT * FROM business_category WHERE type = :type ORDER BY name COLLATE NOCASE ASC")
    fun observeByType(type: String): Flow<List<BusinessCategoryEntity>>

    @Query("SELECT * FROM business_category WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BusinessCategoryEntity?

    @Query("SELECT * FROM business_category WHERE name = :name AND type = :type LIMIT 1")
    suspend fun findByNameAndType(name: String, type: String): BusinessCategoryEntity?

    @Insert
    suspend fun insert(category: BusinessCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: BusinessCategoryEntity)

    @Delete
    suspend fun delete(category: BusinessCategoryEntity)
}
