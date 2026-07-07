package com.example.commingsoon.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class AppThemeViewModel (initialDarkMode: Boolean = false) : ViewModel() {
    var currentTheme by mutableStateOf(AppThemeType.PINK)
        private set
    var darkMode by mutableStateOf(initialDarkMode)
        private set

    fun setTheme(theme: AppThemeType) {
        currentTheme = theme
    }
   fun getThemeDefinition(): AppThemeDefinition {
        return themes[currentTheme]!!
    }

    fun setMode(isDarkMode: Boolean) {
        darkMode = isDarkMode
    }

    val themes = mapOf(
        AppThemeType.VIOLET to VioletTheme,
        AppThemeType.TEAL to TealTheme,
        AppThemeType.PINK to PinkTheme,
        AppThemeType.ORANGE to OrangeTheme,
        AppThemeType.DARKBLUE to DarkBlueTheme,
        AppThemeType.LIGHTBLUE to LightBlueTheme,
        AppThemeType.DARKGREEN to DarkGreenTheme,
        AppThemeType.LIGHTGREEN to LightGreenTheme,
        AppThemeType.YELLOW to YellowTheme
    )
}