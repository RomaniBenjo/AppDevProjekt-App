package com.example.comingsoon.overlays

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.comingsoon.viewmodels.Friend
import com.example.comingsoon.viewmodels.Journey

enum class OverlayType {
    NONE,
    SHARE_JOURNEY,
    SHARE_WITH_FRIEND,
    ADD_FRIEND
}

class OverlayViewModel : ViewModel() {
    var overlayType by mutableStateOf(OverlayType.NONE)
        private set

    var selectedJourney by mutableStateOf<Journey?>(null)
        private set

    var selectedFriend by mutableStateOf<Friend?>(null)
        private set

    fun showJourneyShare(journey: Journey) {
        selectedJourney = journey
        overlayType = OverlayType.SHARE_JOURNEY
    }

    fun showFriendShare(friend: Friend) {
        selectedFriend = friend
        overlayType = OverlayType.SHARE_WITH_FRIEND
    }

    fun showAddFriend() {
        overlayType = OverlayType.ADD_FRIEND
    }

    fun dismiss() {
        selectedJourney = null
        selectedFriend = null
        overlayType = OverlayType.NONE
    }
}