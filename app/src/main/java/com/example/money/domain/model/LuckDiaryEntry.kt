package com.example.money.domain.model

data class LuckDiaryEntry(
    val id: Long = 0,
    val syncId: String = "",
    val entryDate: Long,
    val note: String,
    val amountCents: Long? = null,
    val category: String = "OTHER",
    val createdAt: Long = System.currentTimeMillis(),
)
