package com.example.money.domain.model

data class SavingsGoal(
    val id: Long = 0,
    val syncId: String = "",
    val name: String = "",
    val targetAmountCents: Long,
    val currentAmountCents: Long = 0,
    val currency: String = "USD",
    val updatedAt: Long,
    val deadlineMillis: Long? = null,
) {
    val targetAmount: Double
        get() = targetAmountCents / 100.0

    val remainingAmountCents: Long
        get() = com.example.money.domain.util.MoneyMath.remainingCents(
            targetAmountCents,
            currentAmountCents,
        )

    val progressFraction: Float
        get() = com.example.money.domain.util.MoneyMath.progressFraction(
            targetAmountCents,
            currentAmountCents,
        )

    val progressPercent: Int
        get() = com.example.money.domain.util.GoalProgress.progressPercent(
            targetAmountCents,
            currentAmountCents,
        )

    val isOverAchieved: Boolean
        get() = com.example.money.domain.util.GoalProgress.isOverAchieved(
            targetAmountCents,
            currentAmountCents,
        )

    val surplusAmountCents: Long
        get() = com.example.money.domain.util.GoalProgress.surplusCents(
            targetAmountCents,
            currentAmountCents,
        )

    val dailyGoalCents: Long?
        get() = com.example.money.domain.util.GoalProgress.dailyGoalCents(
            remainingAmountCents,
            deadlineMillis,
        )

    val isCompleted: Boolean
        get() = currentAmountCents >= targetAmountCents && targetAmountCents > 0L
}
