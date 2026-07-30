package com.example.comingsoon.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class JourneyNotificationSchedulerTest {
    @Test
    fun `each journey uses its own unique work name`() {
        assertEquals("journey_reminder_12", journeyReminderWorkName(12))
        assertNotEquals(
            journeyReminderWorkName(12),
            journeyReminderWorkName(13)
        )
    }
}
