package com.example.money.domain.model

data class DebtPayment(
    val id: Long = 0,
    val debtorId: Long,
    val amountCents: Long,
    val currency: String,
    val paidAt: Long,
    val affectsBalance: Boolean = true,
)
