package com.example.money.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.money.data.sync.SyncIds

@Entity(
    tableName = "business_project",
    foreignKeys = [
        ForeignKey(
            entity = BusinessClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["client_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sync_id"], unique = true),
        Index(value = ["client_id"]),
    ],
)
data class BusinessProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "sync_id")
    val syncId: String = SyncIds.newId(),
    @ColumnInfo(name = "client_id")
    val clientId: Long,
    val name: String,
    val status: String = "ACTIVE",
    @ColumnInfo(name = "budget_cents")
    val budgetCents: Long = 0,
    val note: String = "",
)
