package com.example.money.domain.model

data class Transfer(
    val id: Long = 0,
    val syncId: String = "",
    val amount: Double,
    val date: Long,
    val isConfirmed: Boolean = false,
    val goalId: Long? = null,
    val debtId: Long? = null,
    val debtPaymentId: Long? = null,
    val note: String = "",
)
