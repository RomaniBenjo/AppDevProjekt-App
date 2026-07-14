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
    }

    @Test
    fun distanceAndPointsScaleConsistently() {
        val vienna = GuessLocation(48.2082, 16.3738)
        val berlin = GuessLocation(52.5200, 13.4050)
        val distance = localGuesserDistanceKm(vienna, berlin)

        assertTrue(distance in 520.0..530.0)
        assertTrue(localGuesserPoints(distance) in 3_800..3_900)
        assertTrue(localGuesserPoints(2_000.0) < localGuesserPoints(1_000.0))
    }

    @Test
    fun missedGuessAwardsZeroPoints() {
        assertEquals(0, localGuesserPoints(null))
    }
}
