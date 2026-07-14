package com.example.money.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.money.data.sync.SyncIds

@Entity(
    tableName = "business_invoice",
    foreignKeys = [
        ForeignKey(
            entity = BusinessClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["client_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = BusinessProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["sync_id"], unique = true),
        Index(value = ["client_id"]),
        Index(value = ["project_id"]),
        Index(value = ["status"]),
    ],
)
data class BusinessInvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "sync_id")
    val syncId: String = SyncIds.newId(),
    @ColumnInfo(name = "client_id")
    val clientId: Long,
    @ColumnInfo(name = "project_id")
    val projectId: Long? = null,
    @ColumnInfo(name = "issue_date")
    val issueDate: Long,
    @ColumnInfo(name = "due_date")
    val dueDate: Long,
    @ColumnInfo(name = "amount_cents")
    val amountCents: Long,
    val status: String = "DRAFT",
    val note: String = "",
    val currency: String = "USD",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
