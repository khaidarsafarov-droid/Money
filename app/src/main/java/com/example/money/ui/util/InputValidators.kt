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

    /**
     * Percent in the closed range 0..100, returned as basis points (12.5% → 1250).
     * A blank field is optional and returns 0. Comma and dot are both accepted.
     * Out-of-range, negative, and non-numeric text returns null.
     */
    fun parsePercent(text: String): Int? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        val normalized = trimmed.replace(',', '.')
        if (normalized.any { character -> character == 'e' || character == 'E' || character.isLetter() }) {
            return null
        }
        val decimal = try {
            BigDecimal(normalized)
        } catch (_: NumberFormatException) {
            return null
        }
        if (decimal.signum() < 0) return null
        val basisPoints = try {
            decimal.setScale(2, RoundingMode.HALF_UP).movePointRight(2).intValueExact()
        } catch (_: ArithmeticException) {
            return null
        }
        if (basisPoints < 0 || basisPoints > 10_000) return null
        return basisPoints
    }

    @StringRes
    fun validatePercent(text: String): Int? = when {
        text.isBlank() -> null
        parsePercent(text) == null -> R.string.error_percent_range
        else -> null
    }

    fun formatBasisPoints(basisPoints: Int): String {
        if (basisPoints <= 0) return ""
        val whole = basisPoints / 100
        val fraction = basisPoints % 100
        return when {
            fraction == 0 -> whole.toString()
            fraction % 10 == 0 -> "$whole.${fraction / 10}"
            else -> "$whole.${fraction.toString().padStart(2, '0')}"
        }
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
