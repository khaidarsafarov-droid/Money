package com.example.money.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.money.data.sync.SyncIds

@Entity(
    tableName = "finance_notes",
    indices = [Index(value = ["sync_id"], unique = true)],
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
)
