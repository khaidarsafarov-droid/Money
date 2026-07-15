package com.example.money.messaging

enum class MessagingChannel {
    SMS,
    WHATSAPP,
    TELEGRAM,
    EMAIL,
}

data class MessagingContact(
    val phone: String = "",
    val email: String = "",
    val name: String = "",
)

data class MessagePayload(
    val subject: String = "",
    val body: String,
)
