package com.example.money.settings

enum class AppMode {
    PERSONAL,
    PROFESSIONAL,
    ;

    companion object {
        fun fromStored(value: String?): AppMode =
            entries.firstOrNull { it.name == value } ?: PERSONAL
    }
}
