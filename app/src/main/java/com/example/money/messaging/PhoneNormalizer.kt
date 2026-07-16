package com.example.money.messaging

object PhoneNormalizer {
    fun normalize(raw: String): String? {
        val digits = raw.filter { it.isDigit() || it == '+' }
        if (digits.isBlank()) return null
        return digits.trim()
    }
}
