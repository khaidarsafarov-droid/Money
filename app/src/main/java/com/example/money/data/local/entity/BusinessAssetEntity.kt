package com.example.money.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.money.data.sync.SyncIds

@Entity(
    tableName = "business_asset",
    indices = [Index(value = ["sync_id"], unique = true)],
)
data class BusinessAssetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "sync_id")
    val syncId: String = SyncIds.newId(),
    val name: String,
    val type: String = "BANK",
    @ColumnInfo(name = "balance_cents")
    val balanceCents: Long = 0,
    val currency: String = "USD",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
