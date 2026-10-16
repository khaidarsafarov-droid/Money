package com.example.money.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.money.data.sync.SyncIds

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["sync_id"], unique = true),
        Index(value = ["isConfirmed"]),
        Index(value = ["debt_id"]),
        Index(value = ["goalId"]),
    ],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "sync_id")
    val syncId: String = SyncIds.newId(),
    @ColumnInfo(name = "amount_cents")
    val amountCents: Long,
    val category: String,
    val date: Long,
    val note: String = "",
    /** Только подтверждённые транзакции учитываются в прогрессе виджета. */
    val isConfirmed: Boolean = false,
    @ColumnInfo(name = "debt_id")
    val debtId: Long? = null,
    @ColumnInfo(name = "debt_payment_id")
    val debtPaymentId: Long? = null,
    /** null — расход из свободных денег; иначе расход из этой цели. */
    val goalId: Long? = null,
    @ColumnInfo(name = "goal_sync_id")
    val goalSyncId: String? = null,
)
