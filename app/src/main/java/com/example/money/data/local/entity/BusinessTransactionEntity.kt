package com.example.money.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.money.data.sync.SyncIds

@Entity(
    tableName = "business_transaction",
    foreignKeys = [
        ForeignKey(
            entity = BusinessClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["client_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = BusinessProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = BusinessInvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoice_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["sync_id"], unique = true),
        Index(value = ["client_id"]),
        Index(value = ["project_id"]),
        Index(value = ["invoice_id"]),
        Index(value = ["category_id"]),
        Index(value = ["date"]),
        Index(value = ["ledger_type"]),
    ],
)
data class BusinessTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "sync_id")
    val syncId: String = SyncIds.newId(),
    val type: String,
    @ColumnInfo(name = "amount_cents")
    val amountCents: Long,
    val category: String,
    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,
    @ColumnInfo(name = "ledger_type")
    val ledgerType: String = "OPERATING",
    val currency: String = "USD",
    @ColumnInfo(name = "payment_method")
    val paymentMethod: String = "OTHER",
    @ColumnInfo(name = "tax_type")
    val taxType: String? = null,
    @ColumnInfo(name = "client_id")
    val clientId: Long? = null,
    @ColumnInfo(name = "project_id")
    val projectId: Long? = null,
    @ColumnInfo(name = "invoice_id")
    val invoiceId: Long? = null,
    val date: Long,
    val note: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
