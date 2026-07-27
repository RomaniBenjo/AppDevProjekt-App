package com.example.comingsoon.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JourneyShareLinkTest {
    @Test
    fun parsesValidShareLink() {
        val token = "abcDEF0123_-abcDEF0123_-abcDEF0123_-"
        assertEquals(
            token,
            JourneyShareLink.parse("comingsoon://journey-share/$token")
        )
    }

    @Test
    fun rejectsWrongHostAndUnsafeToken() {
        assertNull(JourneyShareLink.parse("comingsoon://friend/123"))
        assertNull(JourneyShareLink.parse("comingsoon://journey-share/too-short"))
        assertNull(
            JourneyShareLink.parse(
                "comingsoon://journey-share/abcDEF0123_-abcDEF0123_-abcDEF0123_-%2F"
            )
        )
    }
}
