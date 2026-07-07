package com.example.money.contacts

data class PickedContact(
    val displayName: String,
    val lookupKey: String,
    val contactUri: String,
    val photoUri: String? = null,
)
