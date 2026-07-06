package com.example.money.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.money.data.sync.SyncIds

@Entity(
    tableName = "savings_goal",
    indices = [Index(value = ["sync_id"], unique = true)],
)
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "sync_id")
    val syncId: String = SyncIds.newId(),
    val name: String = "",
    @ColumnInfo(name = "target_amount_cents")
    val targetAmountCents: Long,
    @ColumnInfo(name = "current_amount_cents")
    val currentAmountCents: Long = 0,
    val currency: String = "USD",
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "deadline_ms")
    val deadlineMillis: Long? = null,
) {
    /** Для обратной совместимости с логикой бюджета (double). */
    val targetAmount: Double
        get() = targetAmountCents / 100.0
}
