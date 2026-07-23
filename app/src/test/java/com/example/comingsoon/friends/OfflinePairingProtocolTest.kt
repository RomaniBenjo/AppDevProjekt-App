package com.example.comingsoon.friends

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflinePairingProtocolTest {
    @Test
    fun `both devices derive the same pairing id regardless of nonce order`() {
        val first = offlinePairingId("nonce-a", "nonce-b")
        val second = offlinePairingId("nonce-b", "nonce-a")

        assertEquals(first, second)
        assertEquals(64, first.length)
        assertTrue(first.all { it in "0123456789abcdef" })
    }

    @Test
    fun `a new nearby session creates a different pairing id`() {
        assertNotEquals(
            offlinePairingId("nonce-a", "nonce-b"),
            offlinePairingId("nonce-a", "nonce-c")
        )
    }
}
