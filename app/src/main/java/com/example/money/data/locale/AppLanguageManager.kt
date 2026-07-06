package com.example.money.data.locale

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.money.settings.AppLanguage
import com.example.money.ui.util.findActivity

object AppLanguageManager {

    fun apply(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(language.toLocaleList())
    }

    fun applyStored(context: Context) {
        apply(AppLanguageStorage.read(context))
    }

    fun persistAndApply(context: Context, language: AppLanguage) {
        AppLanguageStorage.write(context, language)
        apply(language)
        context.findActivity()?.recreate()
    }

    private fun AppLanguage.toLocaleList(): LocaleListCompat = when (this) {
        AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
        AppLanguage.RU -> LocaleListCompat.forLanguageTags("ru-RU")
        AppLanguage.EN -> LocaleListCompat.forLanguageTags("en-US")
    }
}
