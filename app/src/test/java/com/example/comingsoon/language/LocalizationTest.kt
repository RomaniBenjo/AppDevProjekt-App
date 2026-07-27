package com.example.comingsoon.language

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LocalizationTest {
    @Test
    fun countryNamesFollowSelectedLanguageForIsoAndSvgNames() {
        val german = Locale.GERMAN
        val english = Locale.ENGLISH

        assertEquals("Deutschland", localizedCountryName("DE", "Germany", german))
        assertEquals("Germany", localizedCountryName("DE", "Germany", english))
        assertEquals("Frankreich", localizedCountryName("France", "France", german))
        assertEquals("France", localizedCountryName("France", "France", english))
        assertEquals(
            "Vereinigte Staaten",
            localizedCountryName("United States", "United States", german)
        )
        assertEquals(
            "Russland",
            localizedCountryName("Russian Federation", "Russian Federation", german)
        )
    }
}
