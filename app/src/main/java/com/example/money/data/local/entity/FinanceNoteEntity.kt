package com.example.money.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.money.data.sync.SyncIds
import com.example.money.settings.AppMode

@Entity(
    tableName = "finance_notes",
    indices = [
        Index(value = ["sync_id"], unique = true),
        Index(value = ["mode"]),
        Index(value = ["deleted_at", "archived", "pinned", "updated_at"]),
    ],
)
data class FinanceNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val kind: String,
    val body: String = "",
    val mark: String = "NONE",
    val closed: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "sync_id")
    val syncId: String = SyncIds.newId(),
    @ColumnInfo(name = "mode", defaultValue = "PERSONAL")
    val mode: String = AppMode.PERSONAL.name,
    @ColumnInfo(name = "pinned", defaultValue = "0")
    val pinned: Boolean = false,
    @ColumnInfo(name = "archived", defaultValue = "0")
    val archived: Boolean = false,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
    @ColumnInfo(name = "linked_type")
    val linkedType: String? = null,
    @ColumnInfo(name = "linked_sync_id")
    val linkedSyncId: String? = null,
)
