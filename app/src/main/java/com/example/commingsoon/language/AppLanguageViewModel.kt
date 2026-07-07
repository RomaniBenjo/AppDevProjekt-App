package com.example.commingsoon.language

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class AppLanguageViewModel : ViewModel() {
    var currentLanguage by mutableStateOf(AppLanguage.ENGLISH)
        private set

    fun setLanguage(language: AppLanguage) {
        currentLanguage = language
    }

    fun getCurrentLanguage(): AppLanguage {
        return currentLanguage
    }

    fun getLanguages(): List<AppLanguage> {
        return AppLanguage.entries
    }
}