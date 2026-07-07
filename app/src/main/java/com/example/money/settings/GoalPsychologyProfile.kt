package com.example.money.settings

enum class FinancialPsychotype {
    SPENDER,
    SAVER,
    INDIFFERENT,
    PLAYER,
    BALANCED,
    ;

    companion object {
        fun fromStored(value: String?): FinancialPsychotype =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: BALANCED
    }
}

data class GoalPsychologyProfile(
    val psychotype: FinancialPsychotype? = null,
    val psychotypeAnswersJson: String = "[]",
    val selectedFearIds: Set<String> = emptySet(),
    val wordPowerEnabled: Boolean = true,
    val personalMotivation: String = "",
    val visionPhotoPath: String? = null,
    val paydayDayOfMonth: Int = 25,
    val payoffStrategy: String = "AVALANCHE",
    val weeklyChecklistJson: String = "{}",
    val activeChallengesJson: String = "[]",
    val goalBadges: Set<String> = emptySet(),
    val psychotypeTestCompleted: Boolean = false,
)
