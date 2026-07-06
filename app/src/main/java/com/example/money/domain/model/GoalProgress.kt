package com.example.money.domain.model

data class GoalProgress(
    val targetAmount: Double,
    val balance: Double,
    val spentOrAllocated: Double,
) {
    /** Прогресс = остаток бюджета (переводы − расходы) относительно цели. */
    val progressFraction: Float
        get() = if (targetAmount <= 0) 0f else (balance / targetAmount).toFloat().coerceIn(0f, 1f)

    val remainingAmount: Double
        get() = (targetAmount - balance).coerceAtLeast(0.0)

    val progressPercent: Int
        get() = (progressFraction * 100).toInt()
}
