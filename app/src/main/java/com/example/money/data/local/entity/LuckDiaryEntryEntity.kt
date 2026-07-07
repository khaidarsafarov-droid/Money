package com.example.money.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.money.data.sync.SyncIds

@Entity(
    tableName = "luck_diary_entry",
    indices = [Index(value = ["sync_id"], unique = true), Index(value = ["entry_date"])],
)
data class LuckDiaryEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "sync_id")
    val syncId: String = SyncIds.newId(),
    @ColumnInfo(name = "entry_date")
    val entryDate: Long,
    val note: String,
    @ColumnInfo(name = "amount_cents")
    val amountCents: Long? = null,
    val category: String = "OTHER",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
