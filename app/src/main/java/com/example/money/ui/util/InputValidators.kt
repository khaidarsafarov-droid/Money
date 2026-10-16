package com.example.money.ui.util

import androidx.annotation.StringRes
import com.example.money.R
import java.math.BigDecimal
import java.math.RoundingMode

object InputValidators {

    const val MAX_AMOUNT_CENTS = 1_000_000_000L

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

    fun parseAmount(text: String): Long? {
        val normalized = text.trim().replace(',', '.')
        if (normalized.isEmpty()) return null
        if (normalized.any { character -> character == 'e' || character == 'E' || character.isLetter() }) {
            return null
        }
        val decimal = try {
            BigDecimal(normalized)
        } catch (_: NumberFormatException) {
            return null
        }
        if (decimal.signum() <= 0) return null
        val cents = try {
            decimal.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
        } catch (_: ArithmeticException) {
            return null
        }
        if (cents <= 0L || cents > MAX_AMOUNT_CENTS) return null
        return cents
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
