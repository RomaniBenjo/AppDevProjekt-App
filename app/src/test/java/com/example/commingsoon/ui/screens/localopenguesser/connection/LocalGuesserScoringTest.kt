package com.example.commingsoon.ui.screens.localopenguesser.connection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalGuesserScoringTest {
    @Test
    fun identicalLocationsAwardMaximumPoints() {
        val location = GuessLocation(48.2082, 16.3738)

        assertEquals(0.0, localGuesserDistanceKm(location, location), 0.0001)
        assertEquals(5_000, localGuesserPoints(0.0))
        assertEquals(5_000, localGuesserPoints(5.0))
    }

    @Test
    fun scoreDropsQuicklyBeyondFiveKilometers() {
        assertTrue(localGuesserPoints(6.0) < 5_000)
        assertTrue(localGuesserPoints(25.0) < 4_000)
        assertTrue(localGuesserPoints(100.0) < localGuesserPoints(25.0))
    }

    @Test
    fun scoreIsAtMostTwoThousandFromThreeHundredKilometers() {
        assertEquals(2_000, localGuesserPoints(300.0))
        assertTrue(localGuesserPoints(500.0) < 2_000)
    }

    @Test
    fun distanceCalculationUsesKilometers() {
        val vienna = GuessLocation(48.2082, 16.3738)
        val berlin = GuessLocation(52.5200, 13.4050)
        val distance = localGuesserDistanceKm(vienna, berlin)

        assertTrue(distance in 520.0..530.0)
        assertTrue(localGuesserPoints(distance) < 2_000)
    }

    @Test
    fun missedGuessAwardsZeroPoints() {
        assertEquals(0, localGuesserPoints(null))
    }
}
