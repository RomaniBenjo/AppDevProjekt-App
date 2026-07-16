package com.example.commingsoon.language

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import java.util.Locale
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.compositionLocalOf

class AppLanguageViewModel : ViewModel() {
    var currentLanguage by mutableStateOf(AppLanguage.ENGLISH)
        private set
    fun setLanguage(language: AppLanguage) {
        if (language == currentLanguage) {
            return
        }
        currentLanguage = language
    }

    fun getLanguages(): List<AppLanguage> = AppLanguage.entries
}