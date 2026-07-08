package com.example.commingsoon.overlays

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.commingsoon.viewmodels.Journey

class ShareViewModel : ViewModel() {
    var selectedJourney by mutableStateOf<Journey?>(null)
        private set

    fun show(journey: Journey) {
        selectedJourney = journey
    }

    fun hide() {
        selectedJourney = null
    }
}