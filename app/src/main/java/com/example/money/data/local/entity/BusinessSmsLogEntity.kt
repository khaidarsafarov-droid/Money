package com.example.money.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "business_sms_log",
    foreignKeys = [
        ForeignKey(
            entity = BusinessClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["client_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["client_id"]),
        Index(value = ["created_at"]),
        Index(value = ["status"]),
    ],
)
data class BusinessSmsLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "recipient_phone")
    val recipientPhone: String,
    @ColumnInfo(name = "recipient_name")
    val recipientName: String = "",
    @ColumnInfo(name = "client_id")
    val clientId: Long? = null,
    val body: String,
    @ColumnInfo(name = "template_id")
    val templateId: String? = null,
    val status: String = "PENDING",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
