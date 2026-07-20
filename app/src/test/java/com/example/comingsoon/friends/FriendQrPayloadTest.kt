package com.example.comingsoon.friends

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FriendQrPayloadTest {
    @Test
    fun roundTripReturnsUserId() {
        assertEquals(42, FriendQrPayload.parse(FriendQrPayload.create(42)))
    }

    @Test
    fun rejectsUntrustedOrMalformedPayloads() {
        assertNull(FriendQrPayload.parse("https://example.com/friend/42"))
        assertNull(FriendQrPayload.parse("comingsoon://friend/not-a-number"))
        assertNull(FriendQrPayload.parse("comingsoon://friend/0"))
        assertNull(FriendQrPayload.parse("comingsoon://friend/42?admin=true"))
    }
}
