package com.example.money.domain.model

data class GoalAction(
    val id: Long = 0,
    val goalId: Long,
    val title: String,
    val completed: Boolean = false,
    val sortOrder: Int = 0,
    val occurredAt: Long = 0L,
)
