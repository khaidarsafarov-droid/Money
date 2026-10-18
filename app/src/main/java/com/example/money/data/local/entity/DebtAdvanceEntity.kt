package com.example.money.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.money.data.sync.SyncIds
import com.example.money.settings.AppMode

@Entity(
    tableName = "debt_advance",
    foreignKeys = [
        ForeignKey(
            entity = DebtorEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtor_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("debtor_id"),
        Index(value = ["sync_id"], unique = true),
        Index(value = ["mode"]),
    ],
)
data class DebtAdvanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "sync_id")
    val syncId: String = SyncIds.newId(),
    @ColumnInfo(name = "debtor_id")
    val debtorId: Long,
    @ColumnInfo(name = "amount_cents")
    val amountCents: Long,
    val currency: String,
    @ColumnInfo(name = "given_at")
    val givenAt: Long,
    val note: String = "",
    @ColumnInfo(name = "mode", defaultValue = "PERSONAL")
    val mode: String = AppMode.PERSONAL.name,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
