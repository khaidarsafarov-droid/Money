package com.example.money.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.money.data.local.entity.BusinessAssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessAssetDao {

    @Query("SELECT * FROM business_asset ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<BusinessAssetEntity>>

    @Query("SELECT * FROM business_asset WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BusinessAssetEntity?

    @Insert
    suspend fun insert(asset: BusinessAssetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(asset: BusinessAssetEntity)

    @Delete
    suspend fun delete(asset: BusinessAssetEntity)
}
