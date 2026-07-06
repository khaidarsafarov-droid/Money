package com.example.money.voice

data class VoiceReportSettings(
    val enabled: Boolean = false,
    val reportHour: Int = DEFAULT_HOUR,
    val reportMinute: Int = DEFAULT_MINUTE,
    val language: VoiceLanguage = VoiceLanguage.RU,
    val gender: VoiceGender = VoiceGender.MALE,
    val forceInSilentMode: Boolean = false,
    val lastVoiceReportDate: String? = null,
    val lastAppOpenDate: String? = null,
) {
    val playbackConfig: VoicePlaybackConfig
        get() = VoicePlaybackConfig(
            language = language,
            gender = gender,
            forceInSilentMode = forceInSilentMode,
        )

    companion object {
        const val DEFAULT_HOUR = 8
        const val DEFAULT_MINUTE = 0
    }
}
