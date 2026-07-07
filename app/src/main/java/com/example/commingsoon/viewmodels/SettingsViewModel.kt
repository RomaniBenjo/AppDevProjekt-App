package com.example.commingsoon.viewmodels

import androidx.lifecycle.ViewModel
import com.example.commingsoon.language.AppLanguage
import com.example.commingsoon.language.AppLanguageViewModel
import com.example.commingsoon.ui.theme.AppThemeDefinition
import com.example.commingsoon.ui.theme.AppThemeType
import com.example.commingsoon.ui.theme.AppThemeViewModel

class SettingsViewModel(
    private val languageViewModel: AppLanguageViewModel = AppLanguageViewModel(),
    private val themeViewModel: AppThemeViewModel = AppThemeViewModel()
) : ViewModel() {

    // Dark / Light Mode

    fun getCurrentMode(): Boolean {
        return themeViewModel.getMode()
    }

    fun setDarkLightMode(isDark: Boolean) {
        themeViewModel.setMode(isDark)
    }

    // Theme

    fun getCurrentTheme(): AppThemeType {
        return themeViewModel.getCurrentTheme()
    }

    fun setTheme(theme: AppThemeType) {
        themeViewModel.setTheme(theme)
    }

    fun getCurrentThemeDefinition(): AppThemeDefinition {
        return themeViewModel.getThemeDefinition()
    }

    fun getAllThemes(): List<AppThemeType> {
        return AppThemeType.entries
    }

    // Language

    fun getCurrentLanguage(): AppLanguage {
        return languageViewModel.getCurrentLanguage()
    }

    fun setLanguage(language: AppLanguage) {
        languageViewModel.setLanguage(language)
    }

    fun getAllLanguages(): List<AppLanguage> {
        return languageViewModel.getLanguages()
    }
}