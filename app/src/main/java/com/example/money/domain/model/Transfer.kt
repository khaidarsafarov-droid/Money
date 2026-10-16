package com.example.money.domain.model

import com.example.money.settings.AppMode

data class Transfer(
    val id: Long = 0,
    val syncId: String = "",
    val amountCents: Long,
    val date: Long,
    val isConfirmed: Boolean = false,
    val goalId: Long? = null,
    val debtId: Long? = null,
    val debtPaymentId: Long? = null,
    val note: String = "",
    val mode: AppMode = AppMode.PERSONAL,
)
