package com.example.comingsoon.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun ComingSoonTheme(
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