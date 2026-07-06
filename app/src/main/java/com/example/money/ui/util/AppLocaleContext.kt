package com.example.money.ui.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.example.money.settings.AppLanguage
import java.util.Locale

object AppLocaleContext {

    fun wrap(context: Context, language: AppLanguage): Context {
        val locale = language.toLocale()
        val configuration = Configuration(context.resources.configuration)

        if (locale == null) {
            Locale.setDefault(configuration.primaryLocale())
            return context
        }

        Locale.setDefault(locale)
        configuration.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(LocaleList(locale))
        }
        return context.createConfigurationContext(configuration)
    }

    private fun Configuration.primaryLocale(): Locale =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locales[0]
        } else {
            @Suppress("DEPRECATION")
            locale
        }
}
