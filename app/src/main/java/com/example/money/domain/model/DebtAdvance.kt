package com.example.money.domain.model

import com.example.money.settings.AppMode

data class DebtAdvance(
    val id: Long = 0,
    val syncId: String = "",
    val debtorId: Long,
    val amountCents: Long,
    val currency: String,
    val givenAt: Long,
    val note: String = "",
    val mode: AppMode = AppMode.PERSONAL,
    val updatedAt: Long = givenAt,
)
