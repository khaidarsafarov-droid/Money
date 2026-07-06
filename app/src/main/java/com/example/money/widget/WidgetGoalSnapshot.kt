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
)

enum class WidgetItemType {
    GOAL,
    DEBT,
}

/** Снимок прогресса цели для Glance-виджета. Только локальные данные телефона. */
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
