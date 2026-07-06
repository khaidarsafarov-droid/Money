package com.example.money.ui.util

object InputValidators {

    fun validateGoalPurpose(purpose: String): String? = when {
        purpose.isBlank() -> "Укажите, для чего эта цель"
        purpose.length > 100 -> "Слишком длинное описание"
        else -> null
    }

    fun validateName(name: String): String? = when {
        name.isBlank() -> "Введите имя"
        name.length > 100 -> "Слишком длинное имя"
        else -> null
    }

    fun parseAmount(text: String): Double? {
        val normalized = text.trim().replace(',', '.')
        return normalized.toDoubleOrNull()?.takeIf { it > 0 }
    }

    fun validateAmount(text: String): String? = when {
        text.isBlank() -> "Введите сумму"
        parseAmount(text) == null -> "Сумма должна быть больше 0"
        else -> null
    }

    fun validateCategory(category: String): String? = when {
        category.isBlank() -> "Введите категорию"
        else -> null
    }

    fun validateDueDate(dateGiven: Long, dateDue: Long): String? = when {
        dateDue < dateGiven -> "Срок не может быть раньше даты выдачи"
        else -> null
    }

    fun validateTargetAmount(text: String): String? = when {
        text.isBlank() -> "Введите сумму цели"
        parseAmount(text) == null -> "Сумма должна быть больше 0"
        else -> null
    }
}
