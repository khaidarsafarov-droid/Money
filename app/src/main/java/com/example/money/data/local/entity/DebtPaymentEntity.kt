package com.example.money.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "debt_payment",
    foreignKeys = [
        ForeignKey(
            entity = DebtorEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtor_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("debtor_id"), Index(value = ["sync_id"], unique = true)],
)
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "sync_id")
    val syncId: String = com.example.money.data.sync.SyncIds.newId(),
    @ColumnInfo(name = "debtor_id")
    val debtorId: Long,
    @ColumnInfo(name = "amount_cents")
    val amountCents: Long,
    val currency: String,
    @ColumnInfo(name = "paid_at")
    val paidAt: Long,
    @ColumnInfo(name = "affects_balance")
    val affectsBalance: Boolean = false,
)
