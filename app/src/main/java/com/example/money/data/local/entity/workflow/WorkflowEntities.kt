package com.example.money.data.local.entity.workflow

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "business_workflow_category",
    indices = [Index(value = ["type"])],
)
data class WorkflowCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "business_workflow_process",
    foreignKeys = [
        ForeignKey(
            entity = WorkflowCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["category_id"])],
)
data class WorkflowProcessEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    val name: String,
    val description: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "business_workflow_bill",
    foreignKeys = [
        ForeignKey(
            entity = WorkflowProcessEntity::class,
            parentColumns = ["id"],
            childColumns = ["process_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["process_id"])],
)
data class WorkflowBillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "process_id")
    val processId: Long,
    val title: String,
    @ColumnInfo(name = "total_amount_cents")
    val totalAmountCents: Long,
    val description: String = "",
    @ColumnInfo(name = "period_label")
    val periodLabel: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "business_workflow_participant",
    indices = [Index(value = ["contact_lookup_key"])],
)
data class WorkflowParticipantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    @ColumnInfo(name = "contact_uri")
    val contactUri: String? = null,
    @ColumnInfo(name = "contact_lookup_key")
    val contactLookupKey: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "business_workflow_debt",
    foreignKeys = [
        ForeignKey(
            entity = WorkflowBillEntity::class,
            parentColumns = ["id"],
            childColumns = ["bill_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WorkflowParticipantEntity::class,
            parentColumns = ["id"],
            childColumns = ["participant_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bill_id"]),
        Index(value = ["participant_id"]),
        Index(value = ["status"]),
    ],
)
data class WorkflowDebtEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "bill_id")
    val billId: Long,
    @ColumnInfo(name = "participant_id")
    val participantId: Long,
    @ColumnInfo(name = "amount_cents")
    val amountCents: Long,
    @ColumnInfo(name = "paid_amount_cents")
    val paidAmountCents: Long = 0,
    val status: String = "OPEN",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
