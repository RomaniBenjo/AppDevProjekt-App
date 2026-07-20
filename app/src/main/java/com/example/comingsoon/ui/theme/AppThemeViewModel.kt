package com.example.comingsoon.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comingsoon.R
import com.example.comingsoon.data.AppPreferenceRepository
import kotlinx.coroutines.launch

class AppThemeViewModel (
    private val repository: AppPreferenceRepository
) : ViewModel() {
    var currentTheme by mutableStateOf(AppThemeType.PINK)
        private set
    var darkMode by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            repository.settingsFlow.collect { settings ->
                currentTheme = settings.theme
                darkMode = settings.darkMode
            }
        }
    }

    fun isDarkMode() = darkMode

    fun updateTheme(theme: AppThemeType) {
        currentTheme = theme

        viewModelScope.launch { repository.saveTheme(theme) }
    }
   fun getThemeDefinition(theme: AppThemeType): AppThemeDefinition {
        return themes[theme]!!
    }
    fun getThemeDefinition(): AppThemeDefinition {
        return themes[currentTheme]!!
    }

    fun updateMode(isDarkMode: Boolean) {
        darkMode = isDarkMode

        viewModelScope.launch { repository.saveDarkMode(isDarkMode) }
    }

    fun getThemeName(theme: AppThemeType): Int {
        return themeNames[theme]!!
    }

    fun getAllThemes(): List<AppThemeType> {
        return AppThemeType.entries
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
}