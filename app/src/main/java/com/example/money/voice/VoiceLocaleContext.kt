package com.example.money.voice

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object VoiceLocaleContext {

    fun wrap(context: Context, language: VoiceLanguage): Context {
        val locale = language.toLocale()
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(android.os.LocaleList(locale))
        }
        return context.createConfigurationContext(configuration)
    }
}
