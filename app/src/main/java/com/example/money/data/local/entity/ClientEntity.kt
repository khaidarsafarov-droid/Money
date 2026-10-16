package com.example.money.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.money.data.sync.SyncIds
import com.example.money.settings.AppMode

@Entity(
    tableName = "client",
    indices = [
        Index(value = ["sync_id"], unique = true),
        Index(value = ["mode"]),
    ],
)
data class ClientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "sync_id")
    val syncId: String = SyncIds.newId(),
    val name: String,
    val phone: String = "",
    val company: String = "",
    val email: String? = null,
    @ColumnInfo(name = "last_meeting_at")
    val lastMeetingAt: Long? = null,
    val note: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "sync_updated_at")
    val syncUpdatedAt: Long,
    val deleted: Boolean = false,
    @ColumnInfo(name = "mode", defaultValue = "PROFESSIONAL")
    val mode: String = AppMode.PROFESSIONAL.name,
)
