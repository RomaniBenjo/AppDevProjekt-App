package com.example.commingsoon.language

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import java.util.Locale
import androidx.appcompat.app.AppCompatDelegate

class AppLanguageViewModel : ViewModel() {
    var currentLanguage by mutableStateOf(getCurrentLanguage())
        private set
    fun setLanguage(language: AppLanguage) {
        if (language == currentLanguage) return
        currentLanguage = language
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.languageTag))
    }

    fun getLanguages(): List<AppLanguage> = AppLanguage.entries
    private fun getCurrentLanguage(): AppLanguage {
        val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags()

        return AppLanguage.entries.firstOrNull { it.languageTag == tag } ?: AppLanguage.ENGLISH
    }
}