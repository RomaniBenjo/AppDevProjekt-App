package com.example.commingsoon.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun CommingSoonTheme(
    theme: AppThemeDefinition,
    darkTheme: Boolean,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) theme.colorScheme.dark else theme.colorScheme.light,
        typography = theme.typography,
        content = content
    )
}