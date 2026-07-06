package com.example.money.settings

import java.util.Locale

enum class AppLanguage {
    SYSTEM,
    RU,
    EN,
    ;

    fun toLocale(): Locale? = when (this) {
        SYSTEM -> null
        RU -> Locale("ru", "RU")
        EN -> Locale.US
    }

    companion object {
        fun fromStored(value: String?): AppLanguage = when (value?.uppercase()) {
            "RU" -> RU
            "EN" -> EN
            else -> SYSTEM
        }
    }
}
