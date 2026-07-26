package com.example.money.data.locale

import android.content.Context
import com.example.money.settings.AppLanguage

object AppLanguageStorage {
    private const val PREFS_NAME = "app_locale_prefs"
    private const val KEY_LANGUAGE = "app_language"

    private fun prefs(context: Context) =
        (context.applicationContext ?: context).getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun read(context: Context): AppLanguage {
        val stored = prefs(context).getString(KEY_LANGUAGE, null)
        return AppLanguage.fromStored(stored)
    }

    fun write(context: Context, language: AppLanguage) {
        prefs(context).edit()
            .putString(KEY_LANGUAGE, language.name)
            .commit()
    }

    fun clear(context: Context) {
        check(prefs(context).edit().remove(KEY_LANGUAGE).commit()) {
            "Unable to clear the stored app language"
        }
    }
}
