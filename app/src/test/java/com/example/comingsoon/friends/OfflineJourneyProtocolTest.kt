package com.example.comingsoon.friends

import com.example.comingsoon.viewmodels.Journey
import com.example.comingsoon.viewmodels.JourneyLocation
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineJourneyProtocolTest {
    @Test
    fun `journey payload preserves all shareable fields`() {
        val journey = Journey(
            id = 42,
            title = "Vienna",
            startDate = LocalDate.of(2026, 8, 1),
            endDate = LocalDate.of(2026, 8, 3),
            locations = listOf(
                JourneyLocation(7, "Stephansdom", 48.2085, 16.3731)
            ),
            visitedCountries = listOf("Austria")
        )

        val payload = journey.toOfflinePayload(
            transferId = "transfer-1",
            sharedAt = "2026-07-30T10:00:00Z"
        )
        val received = payload.toJourney(ownerId = -5)

        assertEquals(journey.title, received.title)
        assertEquals(journey.startDate, received.startDate)
        assertEquals(journey.endDate, received.endDate)
        assertEquals(journey.locations, received.locations)
        assertEquals(journey.visitedCountries, received.visitedCountries)
        assertEquals(-5, received.ownerId)
        assertFalse(received.isOwned)
    }

    @Test
    fun `offline ids are stable and negative`() {
        val first = stableOfflineId("device:abc")
        val second = stableOfflineId("device:abc")

        assertEquals(first, second)
        assertTrue(first < 0)
    }
}
