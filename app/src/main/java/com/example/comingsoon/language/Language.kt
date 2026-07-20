package com.example.comingsoon.language

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import android.content.res.Configuration
import java.util.Locale

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ENGLISH }

val LocalLocalizedContext = staticCompositionLocalOf<Context> { error("Localized Context not provided") }

fun Context.localized(language: AppLanguage): Context {
    val configuration = Configuration(resources.configuration)
    configuration.setLocale(Locale.forLanguageTag(language.languageTag))
    return createConfigurationContext(configuration)
}