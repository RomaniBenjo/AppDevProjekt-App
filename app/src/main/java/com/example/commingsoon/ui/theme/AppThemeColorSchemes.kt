package com.example.commingsoon.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

data class AppThemePalette(
    val light: ColorScheme,
    val dark: ColorScheme
)

val VioletColorScheme = AppThemePalette (
    light = lightColorScheme(
        primary = PurpleNormal,
        secondary = PurpleLight,
        tertiary = PurpleDark
    ),
    dark = darkColorScheme (
        primary = PurpleNormal,
        secondary = PurpleDark,
        tertiary = PurpleLight
    )
)

val DarkBlueColorScheme = AppThemePalette(
    light = lightColorScheme (
        primary = Blue2Normal,
        secondary = Blue2Light,
        tertiary = Blue2Dark
    ),
    dark = darkColorScheme (
        primary = Blue2Normal,
    secondary = Blue2Dark,
    tertiary = Blue2Light
    )
)

val LightBlueColorScheme = AppThemePalette (
    light = lightColorScheme(
        primary = Blue1Normal,
        secondary = Blue1Light,
        tertiary = Blue1Dark
    ),
    dark = darkColorScheme(
        primary = Blue1Normal,
        secondary = Blue1Dark,
        tertiary = Blue1Light
    )
)

val TealColorScheme = AppThemePalette (
    light = lightColorScheme(
        primary = TealNormal,
        secondary = TealLight,
        tertiary = TealDark
    ),
    dark = darkColorScheme(
        primary = TealNormal,
        secondary = TealDark,
        tertiary = TealLight
    )
)

val DarkGreenColorScheme = AppThemePalette(
    light = lightColorScheme(
        primary = Green2Normal,
        secondary = Green2Light,
        tertiary = Green2Dark
    ),
    dark = darkColorScheme(
        primary = Green2Normal,
        secondary = Green2Dark,
        tertiary = Green2Light
    )
)

val LightGreenColorScheme = AppThemePalette(
    light = lightColorScheme(
        primary = Green1Normal,
        secondary = Green1Light,
        tertiary = Green1Dark
    ),
    dark = darkColorScheme(
        primary = Green1Normal,
        secondary = Green1Dark,
        tertiary = Green1Light
    )
)

val YellowColorScheme = AppThemePalette(
    light = lightColorScheme(
        primary = YellowNormal,
        secondary = YellowLight,
        tertiary = YellowDark
    ),
    dark = darkColorScheme(
        primary = YellowNormal,
        secondary = YellowDark,
        tertiary = YellowLight
    )
)

val OrangeColorScheme = AppThemePalette(
    light = lightColorScheme(
        primary = OrangeNormal,
        secondary = OrangeLight,
        tertiary = OrangeDark
    ),
    dark = darkColorScheme(
        primary = OrangeNormal,
        secondary = OrangeDark,
        tertiary = OrangeLight
    )
)

val PinkColorScheme = AppThemePalette(
    light = lightColorScheme(
        primary = PinkNormal,
        secondary = PinkLight,
        tertiary = PinkDark
    ),
    dark = darkColorScheme(
        primary = PinkNormal,
        secondary = PinkDark,
        tertiary = PinkLight
    )
)