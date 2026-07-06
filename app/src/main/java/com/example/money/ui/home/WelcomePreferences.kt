package com.example.money.ui.home

import android.content.Context

object WelcomePreferences {

    private const val PREFS = "money_prefs"
    private const val KEY_WELCOME_SEEN = "welcome_seen"

    fun shouldShowWelcome(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return !prefs.getBoolean(KEY_WELCOME_SEEN, false)
    }

    fun markWelcomeSeen(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_WELCOME_SEEN, true)
            .apply()
    }
}
