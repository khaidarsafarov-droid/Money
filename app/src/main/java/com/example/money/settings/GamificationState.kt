package com.example.money.settings

data class GamificationState(
    val streakCount: Int = 0,
    val lastPaymentIsoDate: String? = null,
    val unlockedBadges: Set<String> = emptySet(),
    val lastWinMessage: String? = null,
)
