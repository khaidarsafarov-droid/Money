package com.example.money.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.money.data.sync.SyncIds

@Entity(
    tableName = "debtor",
    indices = [Index(value = ["sync_id"], unique = true)],
)
data class DebtorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "sync_id")
    val syncId: String = SyncIds.newId(),
    val name: String,
    @ColumnInfo(name = "target_amount_cents")
    val targetAmountCents: Long,
    @ColumnInfo(name = "current_amount_cents")
    val currentAmountCents: Long = 0,
    val currency: String = "USD",
    val status: String = "ACTIVE",
    @ColumnInfo(name = "debt_type")
    val debtType: String = "CREDITOR",
    @ColumnInfo(name = "affects_balance")
    val affectsBalance: Boolean = false,
    val note: String = "",
    @ColumnInfo(name = "date_given")
    val dateGiven: Long,
    @ColumnInfo(name = "date_due")
    val dateDue: Long,
    @ColumnInfo(name = "loan_category")
    val loanCategory: String = "OTHER",
    @ColumnInfo(name = "interest_rate_bps")
    val interestRateBps: Int = 0,
)
