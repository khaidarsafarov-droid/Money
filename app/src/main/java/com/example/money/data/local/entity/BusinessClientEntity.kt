package com.example.money.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.money.data.sync.SyncIds

@Entity(
    tableName = "business_client",
    indices = [Index(value = ["sync_id"], unique = true)],
)
data class BusinessClientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "sync_id")
    val syncId: String = SyncIds.newId(),
    val name: String,
    @ColumnInfo(name = "contact_info")
    val contactInfo: String = "",
    val email: String = "",
    val phone: String = "",
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    val note: String = "",
    val tags: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
