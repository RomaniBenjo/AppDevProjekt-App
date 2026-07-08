package com.example.commingsoon.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.commingsoon.language.AppLanguageViewModel
import com.example.commingsoon.ui.theme.AppThemeViewModel

class SettingsViewModelFactory (
    private val languageViewModel: AppLanguageViewModel,
    private val themeViewModel: AppThemeViewModel
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(
            languageViewModel,
            themeViewModel
        ) as T
    }
}