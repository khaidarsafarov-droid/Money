package com.example.money.settings

enum class UserGender {
    MALE,
    FEMALE,
    UNSPECIFIED,
    ;

    companion object {
        fun fromStored(value: String?): UserGender =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNSPECIFIED
    }
}

enum class UserEmotion {
    HAPPY,
    NEUTRAL,
    SAD,
    ANGRY,
    THINKING,
    ;

    companion object {
        fun fromStored(value: String?): UserEmotion? =
            value?.let { stored -> entries.firstOrNull { it.name.equals(stored, ignoreCase = true) } }
    }
}

data class UserProfile(
    val displayName: String = "",
    val gender: UserGender = UserGender.UNSPECIFIED,
    val age: Int? = null,
    val customFear: String = "",
    val goalText: String = "",
    val emotion: UserEmotion? = null,
    val priority: String = "",
    val onboardingCompleted: Boolean = false,
) {
    fun resolvedDisplayName(): String =
        displayName.trim().takeIf { it.length >= 2 } ?: DEFAULT_DISPLAY_NAME

    fun resolvedGoalText(): String =
        goalText.trim().takeIf { it.isNotEmpty() } ?: DEFAULT_GOAL_TEXT

    companion object {
        const val DEFAULT_DISPLAY_NAME = "Друг"
        const val DEFAULT_GOAL_TEXT = "Стать финансово свободным"
    }
}
