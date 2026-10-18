package com.example.money.widget

import com.example.money.settings.CurrencyRegion

/** Снимок целей и долгов для Glance-виджета. Только локальные данные телефона. */
data class WidgetGoalSnapshot(
    val currentAmount: Double,
    val targetAmount: Double,
    val progressFraction: Float,
    val progressPercent: Int,
    val remainingAmount: Double,
    val pendingCount: Int,
    val updatedAt: Long,
    val currencyRegion: CurrencyRegion,
    val totalAccumulated: Double = 0.0,
    val totalRemainingToGoal: Double = 0.0,
    val owedToMeCents: Long = 0L,
    val owedToMeCount: Int = 0,
    val iOweCents: Long = 0L,
    val iOweCount: Int = 0,
    val activeGoalsCount: Int = 0,
    val overdueDebtCount: Int = 0,
    val hideAmounts: Boolean = false,
    val streakCount: Int = 0,
    val lastWinMessage: String? = null,
    /** True when [progressPercent] is the home debt score, otherwise the goal score. */
    val scoreUsesDebts: Boolean = false,
) {
    companion object {
        val Empty = WidgetGoalSnapshot(
            currentAmount = 0.0,
            targetAmount = 0.0,
            progressFraction = 0f,
            progressPercent = 0,
            remainingAmount = 0.0,
            pendingCount = 0,
            updatedAt = 0L,
            currencyRegion = CurrencyRegion.US,
        )
    }
}
