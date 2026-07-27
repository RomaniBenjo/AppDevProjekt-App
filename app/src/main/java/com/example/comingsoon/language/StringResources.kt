package com.example.comingsoon.language

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun appString(
    @StringRes id: Int,
    vararg args: Any
): String {
    return LocalLocalizedContext.current.resources.getString(id, *args)
}

@Composable
fun appQuantityString(
    @PluralsRes id: Int,
    quantity: Int,
    vararg args: Any = arrayOf(quantity)
): String {
    return LocalLocalizedContext.current.resources.getQuantityString(id, quantity, *args)
}

@Composable
fun appDateString(date: LocalDate): String {
    val locale = Locale.forLanguageTag(LocalAppLanguage.current.languageTag)
    return date.format(
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    )
}
