package com.example.comingsoon.language

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import android.content.res.Configuration
import androidx.annotation.StringRes
import java.util.Locale

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ENGLISH }

val LocalLocalizedContext = staticCompositionLocalOf<Context> { error("Localized Context not provided") }

fun Context.localized(language: AppLanguage): Context {
    val configuration = Configuration(resources.configuration)
    configuration.setLocale(Locale.forLanguageTag(language.languageTag))
    return createConfigurationContext(configuration)
}

private const val LANGUAGE_PREFERENCES = "app_language"
private const val LANGUAGE_KEY = "selected_language"

fun Context.persistAppLanguage(language: AppLanguage) {
    getSharedPreferences(LANGUAGE_PREFERENCES, Context.MODE_PRIVATE)
        .edit()
        .putString(LANGUAGE_KEY, language.name)
        .apply()
}

fun Context.persistedAppLanguage(): AppLanguage {
    val value = getSharedPreferences(LANGUAGE_PREFERENCES, Context.MODE_PRIVATE)
        .getString(LANGUAGE_KEY, null)
    return runCatching { value?.let(AppLanguage::valueOf) }
        .getOrNull()
        ?: AppLanguage.ENGLISH
}

fun Context.localizedString(@StringRes id: Int, vararg args: Any): String =
    localized(persistedAppLanguage()).getString(id, *args)

fun localizedCountryName(
    countryId: String,
    fallbackName: String?,
    locale: Locale
): String {
    val aliases = mapOf(
        "russian federation" to "RU",
        "united states of america" to "US",
        "south korea" to "KR",
        "north korea" to "KP",
        "ivory coast" to "CI",
        "czech republic" to "CZ",
        "cape verde" to "CV",
        "laos" to "LA",
        "syria" to "SY",
        "iran" to "IR",
        "moldova" to "MD",
        "tanzania" to "TZ",
        "bolivia" to "BO",
        "venezuela" to "VE"
    )
    val candidates = listOfNotNull(countryId, fallbackName)
    val region = countryId
        .takeIf { it.length == 2 && it.all(Char::isLetter) }
        ?.uppercase(Locale.ROOT)
        ?: candidates.firstNotNullOfOrNull { aliases[it.lowercase(Locale.ROOT)] }
        ?: Locale.getISOCountries().firstOrNull { code ->
            val englishName = Locale.Builder()
                .setRegion(code)
                .build()
                .getDisplayCountry(Locale.ENGLISH)
            candidates.any { it.equals(englishName, ignoreCase = true) }
        }
    return region
        ?.let {
            Locale.Builder().setRegion(it).build().getDisplayCountry(locale)
        }
        ?.takeIf { it.isNotBlank() }
        ?: fallbackName
        ?: countryId
}
