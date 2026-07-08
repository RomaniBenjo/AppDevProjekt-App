package com.example.commingsoon.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

data class AppThemePalette(
    val light: ColorScheme,
    val dark: ColorScheme
)

val VioletColorScheme = AppThemePalette (
    light = lightColorScheme(
        primary = PurpleNormal,
        secondary = PurpleLight,
        tertiary = PurpleDark,
        background = Color.White,
        onBackground = Color.Black
    ),
    dark = darkColorScheme (
        primary = PurpleNormal,
        secondary = PurpleDark,
        tertiary = PurpleLight,
        background = Color.Black,
        onBackground = Color.White
    )
)

val DarkBlueColorScheme = AppThemePalette(
    light = lightColorScheme (
        primary = Blue2Normal,
        secondary = Blue2Light,
        tertiary = Blue2Dark,
        background = Color.White,
        onBackground = Color.Black
    ),
    dark = darkColorScheme (
        primary = Blue2Normal,
        secondary = Blue2Dark,
        tertiary = Blue2Light,
        background = Color.Black,
        onBackground = Color.White
    )
)

val LightBlueColorScheme = AppThemePalette (
    light = lightColorScheme(
        primary = Blue1Normal,
        secondary = Blue1Light,
        tertiary = Blue1Dark,
        background = Color.White,
        onBackground = Color.Black
    ),
    dark = darkColorScheme(
        primary = Blue1Normal,
        secondary = Blue1Dark,
        tertiary = Blue1Light,
        background = Color.Black,
        onBackground = Color.White
    )
)

val TealColorScheme = AppThemePalette (
    light = lightColorScheme(
        primary = TealNormal,
        secondary = TealLight,
        tertiary = TealDark,
        background = Color.White,
        onBackground = Color.Black
    ),
    dark = darkColorScheme(
        primary = TealNormal,
        secondary = TealDark,
        tertiary = TealLight,
        background = Color.Black,
        onBackground = Color.White
    )
)

val DarkGreenColorScheme = AppThemePalette(
    light = lightColorScheme(
        primary = Green2Normal,
        secondary = Green2Light,
        tertiary = Green2Dark,
        background = Color.White,
        onBackground = Color.Black
    ),
    dark = darkColorScheme(
        primary = Green2Normal,
        secondary = Green2Dark,
        tertiary = Green2Light,
        background = Color.Black,
        onBackground = Color.White
    )
)

val LightGreenColorScheme = AppThemePalette(
    light = lightColorScheme(
        primary = Green1Normal,
        secondary = Green1Light,
        tertiary = Green1Dark,
        background = Color.White,
        onBackground = Color.Black
    ),
    dark = darkColorScheme(
        primary = Green1Normal,
        secondary = Green1Dark,
        tertiary = Green1Light,
        background = Color.Black,
        onBackground = Color.White
    )
)

val YellowColorScheme = AppThemePalette(
    light = lightColorScheme(
        primary = YellowNormal,
        secondary = YellowLight,
        tertiary = YellowDark,
        background = Color.White,
        onBackground = Color.Black
    ),
    dark = darkColorScheme(
        primary = YellowNormal,
        secondary = YellowDark,
        tertiary = YellowLight,
        background = Color.Black,
        onBackground = Color.White
    )
)

val OrangeColorScheme = AppThemePalette(
    light = lightColorScheme(
        primary = OrangeNormal,
        secondary = OrangeLight,
        tertiary = OrangeDark,
        background = Color.White,
        onBackground = Color.Black
    ),
    dark = darkColorScheme(
        primary = OrangeNormal,
        secondary = OrangeDark,
        tertiary = OrangeLight,
        background = Color.Black,
        onBackground = Color.White
    )
)

val PinkColorScheme = AppThemePalette(
    light = lightColorScheme(
        primary = PinkNormal,
        secondary = PinkLight,
        tertiary = PinkDark,
        background = Color.White,
        onBackground = Color.Black
    ),
    dark = darkColorScheme(
        primary = PinkNormal,
        secondary = PinkDark,
        tertiary = PinkLight,
        background = Color.Black,
        onBackground = Color.White
    )
)