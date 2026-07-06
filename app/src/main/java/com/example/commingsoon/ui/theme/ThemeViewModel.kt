package com.example.commingsoon.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class ThemeViewModel : ViewModel() {
    var currentTheme by mutableStateOf(AppTheme)
        private set

    fun setTheme(theme: AppTheme) {
        currentTheme = theme
    }
}