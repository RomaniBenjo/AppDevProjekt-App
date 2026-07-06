package com.example.commingsoon.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography

data class AppThemeDefinition(
    val colorScheme: AppThemePalette,
    val typography: Typography,
    val assets: AppThemeAssets
)

val VioletTheme = AppThemeDefinition (
    colorScheme = VioletColorScheme,
    typography = VioletTypography,
    assets = VioletAssets
)

val DarkBlueTheme = AppThemeDefinition (
    colorScheme = DarkBlueColorScheme,
    typography = DarkBlueTypography,
    assets = DarkBlueAssets
)

val LightBlueTheme = AppThemeDefinition (
    colorScheme = LightBlueColorScheme,
    typography = LightBlueTypography,
    assets = LightBlueAssets
)

val TealTheme = AppThemeDefinition(
    colorScheme = TealColorScheme,
    typography = TealTypography,
    assets = TealAssets
)

val DarkGreenTheme = AppThemeDefinition(
    colorScheme = DarkGreenColorScheme,
    typography = DarkGreenTypography,
    assets = DarkGreenAssets
)

val LightGreenTheme = AppThemeDefinition(
    colorScheme = LightGreenColorScheme,
    typography = LightGreenTypography,
    assets = LightGreenAssets
)

val YellowTheme = AppThemeDefinition(
    colorScheme = YellowColorScheme,
    typography = YellowTypography,
    assets = YellowAssets
)

val OrangeTheme = AppThemeDefinition(
    colorScheme = OrangeColorScheme,
    typography = OrangeTypography,
    assets = OrangeAssets
)

val PinkTheme = AppThemeDefinition(
    colorScheme = PinkColorScheme,
    typography = PinkTypography,
    assets = PinkAssets
)