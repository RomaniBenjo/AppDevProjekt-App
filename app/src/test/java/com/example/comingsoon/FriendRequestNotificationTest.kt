package com.example.comingsoon

import com.example.comingsoon.notifications.newIncomingFriendRequestIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendRequestNotificationTest {
    @Test
    fun `only unseen incoming requests produce notifications`() {
        val newRequestIds = newIncomingFriendRequestIds(
            knownRequestIds = setOf(10),
            currentRequestIds = listOf(10, 11)
        )

        assertEquals(setOf(11), newRequestIds)
    }

    @Test
    fun `sender side without incoming requests produces no notification`() {
        assertTrue(newIncomingFriendRequestIds(setOf(10), emptyList()).isEmpty())
    }
}
