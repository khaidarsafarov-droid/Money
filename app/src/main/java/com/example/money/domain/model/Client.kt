package com.example.money.domain.model

data class Client(
    val id: Long = 0,
    val syncId: String = "",
    val name: String,
    val phone: String = "",
    val company: String = "",
    val email: String? = null,
    val lastMeetingAt: Long? = null,
    val note: String = "",
    val createdAt: Long = 0L,
    val syncUpdatedAt: Long = 0L,
    val debtCents: Long = 0L,
)
