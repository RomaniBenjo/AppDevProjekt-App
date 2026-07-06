package com.example.commingsoon.ui.theme

import com.example.commingsoon.R
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)

val Cinzel = FontFamily(
    Font(R.font.cinzel_regular)
)

val CraftyGirls = FontFamily(
    Font(R.font.crafty_girls_regular)
)

val EmilysCandy = FontFamily(
    Font(R.font.emilys_candy_regular)
)

val Flavors = FontFamily(
    Font(R.font.flavors_regular)
)

val LeagueScript = FontFamily(
    Font(R.font.league_script_regular)
)

val MontserratUnderline = FontFamily(
    Font(R.font.montserrat_underline_regular)
)

val RubikPuddles = FontFamily(
    Font(R.font.rubik_puddles_regular)
)

val RubikScribble = FontFamily(
    Font(R.font.rubik_scribble_regular)
)

val SofadiOne = FontFamily(
    Font(R.font.sofadi_one_regular)
)