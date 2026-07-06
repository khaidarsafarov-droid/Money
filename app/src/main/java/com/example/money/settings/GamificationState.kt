package com.example.money.settings

data class GamificationState(
    val streakCount: Int = 0,
    val lastPaymentIsoDate: String? = null,
    val unlockedBadges: Set<String> = emptySet(),
    val lastWinMessage: String? = null,
)

object GamificationBadges {
    const val FIRST_STEP = "first_step"
    const val HALF_WAY = "half_way"
    const val FREEDOM = "freedom"
    const val STREAK_3 = "streak_3"
}
