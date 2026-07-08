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
    var currentLanguage by mutableStateOf(AppLanguage.ENGLISH)
        private set

    fun setLanguage(language: AppLanguage) {
        Log.d("LanguageVM", "setLanguage: $language")
        currentLanguage = language

        val locale = when(language) {
            AppLanguage.ENGLISH -> Locale("en")
            AppLanguage.GERMAN -> Locale("de")
        }

        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.create(locale)
        )
        Log.d("LanguageVM",AppCompatDelegate.getApplicationLocales().toLanguageTags())
    }

    fun getLanguages(): List<AppLanguage> {
        return AppLanguage.entries
    }
}