package com.example.money.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.money.data.sync.SyncIds

@Entity(
    tableName = "transfers",
    indices = [
        Index(value = ["sync_id"], unique = true),
        Index(value = ["isConfirmed"]),
        Index(value = ["goalId"]),
        Index(value = ["debt_id"]),
    ],
)
data class TransferEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "sync_id")
    val syncId: String = SyncIds.newId(),
    val amount: Double,
    val date: Long,
    /** Только подтверждённые транзакции учитываются в балансе. */
    val isConfirmed: Boolean = false,
    /** null — пополнение общего остатка; иначе перевод в цель. */
    val goalId: Long? = null,
    /** UUID цели для P2P sync (локальный goalId может отличаться на другом устройстве). */
    @ColumnInfo(name = "goal_sync_id")
    val goalSyncId: String? = null,
    @ColumnInfo(name = "debt_id")
    val debtId: Long? = null,
    @ColumnInfo(name = "debt_payment_id")
    val debtPaymentId: Long? = null,
    val note: String = "",
)
