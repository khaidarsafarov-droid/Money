package com.example.money.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class DebtorWithPayments(
    @Embedded val debtor: DebtorEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "debtor_id",
    )
    val payments: List<DebtPaymentEntity>,
)
