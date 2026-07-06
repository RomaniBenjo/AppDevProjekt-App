package com.example.commingsoon.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

private val Violet_LightColorScheme = lightColorScheme (
    primary = PurpleNormal,
    secondary = PurpleLight,
    tertiary = PurpleDark
)
private val Violet_DarkColorScheme = darkColorScheme (
    primary = PurpleNormal,
    secondary = PurpleDark,
    tertiary = PurpleLight
)

private val DarkBlue_LightColorScheme = lightColorScheme (
    primary = Blue2Normal,
    secondary = Blue2Light,
    tertiary = Blue2Dark
)
private val DarkBlue_DarkColorScheme = darkColorScheme (
    primary = Blue2Normal,
    secondary = Blue2Dark,
    tertiary = Blue2Light
)

private val LightBlue_LightColorScheme = lightColorScheme(
    primary = Blue1Normal,
    secondary = Blue1Light,
    tertiary = Blue1Dark
)
private val LightBlue_DarkColorScheme = darkColorScheme(
    primary = Blue1Normal,
    secondary = Blue1Dark,
    tertiary = Blue1Light
)

private val Teal_LightColorScheme = lightColorScheme(
    primary = TealNormal,
    secondary = TealLight,
    tertiary = TealDark
)
private val Teal_DarkColorScheme = darkColorScheme(
    primary = TealNormal,
    secondary = TealDark,
    tertiary = TealLight
)

private val DarkGreen_LightColorScheme = lightColorScheme(
    primary = Green2Normal,
    secondary = Green2Light,
    tertiary = Green2Dark
)
private val DarkGreen_DarkColorScheme = darkColorScheme(
    primary = Green2Normal,
    secondary = Green2Dark,
    tertiary = Green2Light
)

private val LightGreen_LightColorScheme = lightColorScheme(
    primary = Green1Normal,
    secondary = Green1Light,
    tertiary = Green1Dark
)
private val LightGreen_DarkColorScheme = darkColorScheme(
    primary = Green1Normal,
    secondary = Green1Dark,
    tertiary = Green1Light
)

private val Yellow_LightColorScheme = lightColorScheme(
    primary = YellowNormal,
    secondary = YellowLight,
    tertiary = YellowDark
)
private val Yellow_DarkColorScheme = darkColorScheme(
    primary = YellowNormal,
    secondary = YellowDark,
    tertiary = YellowLight
)

private val Orange_LightColorScheme = lightColorScheme(
    primary = OrangeNormal,
    secondary = OrangeLight,
    tertiary = OrangeDark
)
private val Orange_DarkColorScheme = darkColorScheme(
    primary = OrangeNormal,
    secondary = OrangeDark,
    tertiary = OrangeLight
)

private val Pink_LightColorScheme = lightColorScheme(
    primary = PinkNormal,
    secondary = PinkLight,
    tertiary = PinkDark
)
private val Pink_DarkColorScheme = darkColorScheme(
    primary = PinkNormal,
    secondary = PinkDark,
    tertiary = PinkLight
)

@Composable
fun CommingSoonTheme(
    theme: AppTheme,
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
//    val colorScheme = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }
//
//        darkTheme -> DarkColorScheme
//        else -> LightColorScheme
//    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )

    val colorScheme = when(theme) {
        AppTheme.VIOLET -> if (darkTheme) Violet_DarkColorScheme else Violet_LightColorScheme
        AppTheme.DARKBLUE -> if (darkTheme) DarkBlue_DarkColorScheme else DarkBlue_LightColorScheme
        AppTheme.LIGHTBLUE -> if(darkTheme) LightBlue_DarkColorScheme else LightBlue_LightColorScheme
        AppTheme.TEAL -> if (darkTheme) Teal_DarkColorScheme else Teal_LightColorScheme
        AppTheme.DARKGREEN -> if (darkTheme) DarkGreen_DarkColorScheme else DarkGreen_LightColorScheme
        AppTheme.LIGHTGREEN -> if (darkTheme) LightGreen_DarkColorScheme else LightGreen_LightColorScheme
        AppTheme.YELLOW -> if (darkTheme) Yellow_DarkColorScheme else Yellow_LightColorScheme
        AppTheme.ORANGE -> if (darkTheme) Orange_DarkColorScheme else Orange_LightColorScheme
        AppTheme.PINK ->  if (darkTheme) Pink_DarkColorScheme else Pink_LightColorScheme
    }
}