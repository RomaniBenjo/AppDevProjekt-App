package com.example.commingsoon.viewmodels

import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import com.example.commingsoon.language.AppLanguage
import com.example.commingsoon.language.AppLanguageViewModel
import com.example.commingsoon.ui.theme.AppThemeDefinition
import com.example.commingsoon.ui.theme.AppThemeType
import com.example.commingsoon.ui.theme.AppThemeViewModel
import com.example.commingsoon.ui.theme.DarkBlueTheme
import com.example.commingsoon.ui.theme.DarkGreenTheme
import com.example.commingsoon.ui.theme.LightBlueTheme
import com.example.commingsoon.ui.theme.LightGreenTheme
import com.example.commingsoon.ui.theme.OrangeTheme
import com.example.commingsoon.ui.theme.PinkTheme
import com.example.commingsoon.ui.theme.TealTheme
import com.example.commingsoon.ui.theme.VioletTheme
import com.example.commingsoon.ui.theme.YellowTheme
import kotlin.to
import com.example.commingsoon.R

class SettingsViewModel(
    private val languageViewModel: AppLanguageViewModel,
    private val themeViewModel: AppThemeViewModel
) : ViewModel() {

    private val themeNames = mapOf(
        AppThemeType.PINK to R.string.pink_theme,
        AppThemeType.VIOLET to R.string.violet_theme,
        AppThemeType.TEAL to R.string.teal_theme,
        AppThemeType.ORANGE to R.string.orange_theme,
        AppThemeType.DARKBLUE to R.string.darkblue_theme,
        AppThemeType.LIGHTBLUE to R.string.lightblue_theme,
        AppThemeType.DARKGREEN to R.string.darkgreen_theme,
        AppThemeType.LIGHTGREEN to R.string.lightgreen_theme,
        AppThemeType.YELLOW to R.string.yellow_theme
    )

    fun getThemeName(theme: AppThemeType): Int {
        return themeNames[theme]!!
    }
    fun getThemeDefinition(theme: AppThemeType): AppThemeDefinition {
        return themeViewModel.themes[theme]!!
    }

    // Dark & Light Mode
    fun getCurrentMode(): Boolean {
        return themeViewModel.darkMode
    }

    fun setDarkLightMode(isDark: Boolean) {
        themeViewModel.setMode(isDark)
    }

    // Theme settings
    fun getCurrentTheme(): AppThemeType {
        return themeViewModel.currentTheme
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

    // Language settings
    fun getCurrentLanguage(): AppLanguage {
        return languageViewModel.currentLanguage
    }

    fun setLanguage(language: AppLanguage) {
        languageViewModel.setLanguage(language)
    }

    fun getAllLanguages(): List<AppLanguage> {
        return languageViewModel.getLanguages()
    }
}