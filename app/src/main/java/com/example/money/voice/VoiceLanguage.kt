package com.example.money.voice

import java.util.Locale

enum class VoiceLanguage {
    RU,
    EN,
    ;

    fun toLocale(): Locale = when (this) {
        RU -> Locale.forLanguageTag("ru-RU")
        EN -> Locale.US
    }

    companion object {
        fun fromStored(value: String?): VoiceLanguage = when (value?.uppercase()) {
            "EN" -> EN
            else -> RU
        }
    }
}

enum class VoiceGender {
    MALE,
    FEMALE,
    ;

    companion object {
        fun fromStored(value: String?): VoiceGender = when (value?.uppercase()) {
            "FEMALE" -> FEMALE
            else -> MALE
        }
    }
}

data class VoicePlaybackConfig(
    val language: VoiceLanguage = VoiceLanguage.RU,
    val gender: VoiceGender = VoiceGender.MALE,
    val forceInSilentMode: Boolean = false,
)
