package com.example.money.ui.util

import androidx.annotation.StringRes
import com.example.money.R

object InputValidators {

    @StringRes
    fun validateGoalPurpose(purpose: String): Int? = when {
        purpose.isBlank() -> R.string.goal_purpose_error
        purpose.length > 100 -> R.string.error_goal_too_long
        else -> null
    }

    @StringRes
    fun validateName(name: String): Int? = when {
        name.isBlank() -> R.string.error_name_required
        name.length > 100 -> R.string.error_name_too_long
        else -> null
    }

    fun parseAmount(text: String): Double? {
        val normalized = text.trim().replace(',', '.')
        return normalized.toDoubleOrNull()?.takeIf { it > 0 }
    }

    @StringRes
    fun validateAmount(text: String): Int? = when {
        text.isBlank() -> R.string.error_amount_required
        parseAmount(text) == null -> R.string.error_amount_positive
        else -> null
    }

    @StringRes
    fun validateCategory(category: String): Int? = when {
        category.isBlank() -> R.string.error_category_required
        else -> null
    }

    @StringRes
    fun validateDueDate(dateGiven: Long, dateDue: Long): Int? = when {
        dateDue < dateGiven -> R.string.error_due_before_given
        else -> null
    }

    @StringRes
    fun validateTargetAmount(text: String): Int? = when {
        text.isBlank() -> R.string.error_goal_amount_required
        parseAmount(text) == null -> R.string.error_amount_positive
        else -> null
    }
}
