package com.example.money.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.money.data.sync.SyncIds

@Entity(
    tableName = "finance_note_lines",
    foreignKeys = [
        ForeignKey(
            entity = FinanceNoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["note_id"]),
        Index(value = ["sync_id"], unique = true),
    ],
)
data class FinanceNoteLineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "note_id")
    val noteId: Long,
    val label: String,
    @ColumnInfo(name = "amount_cents")
    val amountCents: Long,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "sync_id")
    val syncId: String = SyncIds.newId(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)
