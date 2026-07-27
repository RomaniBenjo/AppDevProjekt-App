package com.example.comingsoon

import com.example.comingsoon.sync.PendingJourneyShareAction
import com.example.comingsoon.sync.effectiveJourneyShareType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JourneyShareStateTest {
    @Test
    fun pendingShareIsShownOptimistically() {
        assertEquals(
            "manual",
            effectiveJourneyShareType(
                remoteShareType = null,
                pendingAction = PendingJourneyShareAction.SHARE
            )
        )
    }

    @Test
    fun pendingUnshareHidesCachedServerShare() {
        assertNull(
            effectiveJourneyShareType(
                remoteShareType = "manual",
                pendingAction = PendingJourneyShareAction.UNSHARE
            )
        )
    }

    @Test
    fun automaticServerShareRemainsReadOnlyWithoutPendingAction() {
        assertEquals(
            "automatic",
            effectiveJourneyShareType(
                remoteShareType = "automatic",
                pendingAction = null
            )
        )
    }
}
