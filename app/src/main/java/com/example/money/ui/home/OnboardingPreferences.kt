package com.example.money.ui.home

import android.content.Context

/** Survives process death independently of the DataStore profile write. */
object OnboardingPreferences {

    private const val PREFS = "money_prefs"
    private const val KEY_COMPLETED = "onboarding_completed"

    fun isCompleted(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_COMPLETED, false)

    fun setCompleted(context: Context, completed: Boolean) {
        check(
            context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_COMPLETED, completed)
                .commit(),
        ) {
            "Unable to store the onboarding state"
        }
    }

    fun markCompleted(context: Context) = setCompleted(context, true)

    fun clear(context: Context) = setCompleted(context, false)
}
