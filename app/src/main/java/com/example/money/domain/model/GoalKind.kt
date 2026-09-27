package com.example.money.domain.model

enum class GoalKind {
    MONEY,
    ACTION,
    ;

    companion object {
        fun fromStored(value: String?): GoalKind =
            entries.firstOrNull { it.name == value } ?: MONEY
    }
}
