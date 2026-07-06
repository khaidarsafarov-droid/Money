package com.example.money.widget

import com.example.money.settings.CurrencyRegion

data class WidgetFinanceItem(
    val id: Long,
    /** String key for React web ?pay= deep links. */
    val payKey: String,
    val type: WidgetItemType,
    val name: String,
    val remainingLabel: String,
    val progressFraction: Float,
    val isOverdue: Boolean = false,
)

enum class WidgetItemType {
    GOAL,
    DEBT,
}

/** Снимок финансов для Glance-виджета. Только локальные данные телефона. */
data class WidgetGoalSnapshot(
    val currentAmount: Double,
    val targetAmount: Double,
    val progressFraction: Float,
    val progressPercent: Int,
    val remainingAmount: Double,
    val pendingCount: Int,
    val updatedAt: Long,
    val currencyRegion: CurrencyRegion,
    val activeItems: List<WidgetFinanceItem> = emptyList(),
    val globalBalance: Double = 0.0,
    val totalDeposits: Double = 0.0,
    val totalExpenses: Double = 0.0,
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
            activeItems = emptyList(),
        )
    }
}
