package com.example.money.domain.model

data class Expense(
    val id: Long = 0,
    val syncId: String = "",
    val amountCents: Long,
    val category: String,
    val date: Long,
    val note: String = "",
    val isConfirmed: Boolean = false,
    val debtId: Long? = null,
    val debtPaymentId: Long? = null,
    /** Set when the expense is taken from a goal. Free-wallet expenses leave this empty. */
    val goalId: Long? = null,
)
