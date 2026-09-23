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
    fun resolvedDisplayName(fallback: String = DEFAULT_DISPLAY_NAME): String {
        val trimmed = displayName.trim()
        val placeholder = trimmed.length < 2 ||
            trimmed == DEFAULT_DISPLAY_NAME ||
            trimmed.equals("Friend", ignoreCase = true)
        return if (placeholder) fallback else trimmed
    }

    fun resolvedGoalText(fallback: String = DEFAULT_GOAL_TEXT): String {
        val trimmed = goalText.trim()
        val placeholder = trimmed.isEmpty() ||
            trimmed == DEFAULT_GOAL_TEXT ||
            trimmed.equals("Become financially free", ignoreCase = true)
        return if (placeholder) fallback else trimmed
    }

    companion object {
        const val DEFAULT_DISPLAY_NAME = "Друг"
        const val DEFAULT_GOAL_TEXT = "Стать финансово свободным"
    }
}
