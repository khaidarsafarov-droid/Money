package com.example.money.ui.home

import android.content.Context

object WelcomePreferences {

    private const val PREFS = "money_prefs"
    private const val KEY_WELCOME_SEEN = "welcome_seen"

    fun shouldShowWelcome(context: Context): Boolean = !isWelcomeSeen(context)

    fun isWelcomeSeen(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_WELCOME_SEEN, false)

    fun markWelcomeSeen(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_WELCOME_SEEN, true)
            .apply()
    }

    fun setWelcomeSeen(context: Context, seen: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_WELCOME_SEEN, seen)
            .commit()
    }
}
