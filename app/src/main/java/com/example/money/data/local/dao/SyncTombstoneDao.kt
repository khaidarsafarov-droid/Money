package com.example.money.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.money.data.local.entity.SyncTombstoneEntity

@Dao
interface SyncTombstoneDao {

    @Query("SELECT * FROM sync_tombstone ORDER BY entity_type, entity_id")
    suspend fun getAll(): List<SyncTombstoneEntity>

    @Query(
        """
        SELECT * FROM sync_tombstone
        WHERE entity_type = :entityType AND entity_id = :entityId
        LIMIT 1
        """,
    )
    suspend fun get(entityType: String, entityId: String): SyncTombstoneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tombstone: SyncTombstoneEntity)

    @Transaction
    suspend fun upsertIfNewer(tombstone: SyncTombstoneEntity) {
        val existing = get(tombstone.entityType, tombstone.entityId)
        if (existing == null || tombstone.syncUpdatedAt >= existing.syncUpdatedAt) {
            upsert(tombstone)
        }
    }

    @Query(
        """
        DELETE FROM sync_tombstone
        WHERE entity_type = :entityType
        AND entity_id = :entityId
        AND sync_updated_at <= :liveSyncUpdatedAt
        """,
    )
    suspend fun deleteIfNotNewer(entityType: String, entityId: String, liveSyncUpdatedAt: Long)

    @Query("DELETE FROM sync_tombstone")
    suspend fun clearAll()
}
