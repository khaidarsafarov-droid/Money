package com.example.money.settings

enum class AutopilotStrategy {
    SNOWBALL,
    AVALANCHE,
    ;

    companion object {
        fun fromStored(value: String?): AutopilotStrategy =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SNOWBALL
    }
}

data class AutopilotSettings(
    val enabled: Boolean = false,
    val dailyAmountCents: Long = 300L,
    val strategy: AutopilotStrategy = AutopilotStrategy.SNOWBALL,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val roundPaymentsToTen: Boolean = false,
)
