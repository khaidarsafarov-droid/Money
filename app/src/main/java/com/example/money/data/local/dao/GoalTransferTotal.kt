package com.example.money.data.local.dao

import androidx.room.ColumnInfo

data class GoalTransferTotal(
    @ColumnInfo(name = "goalId")
    val goalId: Long,
    @ColumnInfo(name = "totalAmount")
    val totalAmount: Double,
)
