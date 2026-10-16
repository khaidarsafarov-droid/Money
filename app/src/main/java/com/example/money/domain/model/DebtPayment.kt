package com.example.money.domain.model

import com.example.money.settings.AppMode

data class DebtPayment(
    val id: Long = 0,
    val debtorId: Long,
    val amountCents: Long,
    val currency: String,
    val paidAt: Long,
    val affectsBalance: Boolean = false,
    val mode: AppMode = AppMode.PERSONAL,
)
