package com.example.money.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "sync_tombstone",
    primaryKeys = ["entity_type", "entity_id"],
)
data class SyncTombstoneEntity(
    @ColumnInfo(name = "entity_type")
    val entityType: String,
    @ColumnInfo(name = "entity_id")
    val entityId: String,
    @ColumnInfo(name = "payload_json")
    val payloadJson: String,
    @ColumnInfo(name = "sync_updated_at")
    val syncUpdatedAt: Long,
)
