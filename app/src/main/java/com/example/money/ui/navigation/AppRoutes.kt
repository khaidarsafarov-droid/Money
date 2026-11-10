package com.example.money.ui.navigation

/** Typed routes for Navigation Compose — replaces boolean screen flags. */
object AppRoutes {
    const val HOME = "home"
    const val GOAL_HUB = "goal_hub"
    const val VOICE_SETTINGS = "voice_settings"
    const val APP_LANGUAGE = "app_language"
    const val PRIVACY_SETTINGS = "privacy_settings"
    const val AUTOPILOT_SETTINGS = "autopilot_settings"
    const val CALENDAR_SETTINGS = "calendar_settings"
    const val DEBT_NOTIFICATION_SETTINGS = "debt_notification_settings"
    const val BACKUP_SETTINGS = "backup_settings"
    const val SYNC_SETTINGS = "sync_settings"
    const val CLIENTS = "clients"
    const val CLIENT_NEW = "clients/new"
    const val CLIENT_EDIT = "clients/{clientId}/edit"
    const val CLIENT_DETAIL = "clients/{clientId}"
    const val GOAL_DETAIL = "goals/{goalId}"

    fun clientDetail(id: Long): String = "clients/$id"

    fun clientEdit(id: Long): String = "clients/$id/edit"

    fun goalDetail(id: Long): String = "goals/$id"
}
