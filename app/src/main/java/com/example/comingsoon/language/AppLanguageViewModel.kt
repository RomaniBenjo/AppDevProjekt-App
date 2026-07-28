package com.example.comingsoon.language

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comingsoon.data.AppPreferenceRepository
import kotlinx.coroutines.launch

class AppLanguageViewModel (
    private val repository: AppPreferenceRepository
) : ViewModel() {
    var currentLanguage by mutableStateOf(AppLanguage.ENGLISH)
        private set

    init {
        viewModelScope.launch {
            repository.settingsFlow.collect {
                currentLanguage = it.language
                repository.cacheLanguage(it.language)
            }
        }
    }
    fun updateLanguage(language: AppLanguage) {
        if (language == currentLanguage) {
            return
        }
        repository.cacheLanguage(language)
        currentLanguage = language

        viewModelScope.launch {
            repository.saveLanguage(language)
        }
    }

    fun getLanguages(): List<AppLanguage> = AppLanguage.entries
}
