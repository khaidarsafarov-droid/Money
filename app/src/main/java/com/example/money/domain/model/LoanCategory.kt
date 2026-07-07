package com.example.money.domain.model

enum class LoanCategory {
    CREDIT_CARD,
    CONSUMER,
    AUTO,
    MORTGAGE,
    EDUCATION,
    OTHER,
    ;

    val isHighPriority: Boolean
        get() = this == CREDIT_CARD || this == CONSUMER

    companion object {
        fun fromStored(value: String?): LoanCategory =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTHER
    }
}
