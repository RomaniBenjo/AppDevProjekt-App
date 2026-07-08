package com.example.commingsoon.viewmodels

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import java.nio.file.Files.copy
import java.time.LocalDate

data class Journey(
    val id: Int,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val shared: Boolean? = null,
    val locations: List<JourneyLocation>
) {
    val pinCount: Int
        get() = locations.size
}

data class JourneyLocation (
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

class JourneyViewModel : ViewModel() {
    private val _journeys = mutableStateListOf<Journey>()

    val journeys: List<Journey>
        get() = _journeys

    init {
        _journeys.addAll(JourneyPlaceholder.journeys)
    }

    // journey management
    fun getJourney(id: Int): Journey? {
        return _journeys.find { it.id == id }
    }
    fun addJourney(journey: Journey) {
        _journeys.add(journey)
    }
    fun removeJourney(id: Int) {
        _journeys.removeIf { it.id == id }
    }
    private fun updateJourney(
        id: Int,
        update: Journey.() -> Journey
    ) {
        val index = _journeys.indexOfFirst { it.id == id }

        if (index != -1) {
            _journeys[index] = _journeys[index].update()
        }
    }

    fun updateTitle(id: Int, title: String) {
        updateJourney(id) {
            copy(title = title)
        }
    }
    fun updateDates(id: Int, startDate: LocalDate, endDate: LocalDate) {
        updateJourney(id) {
            copy(
                startDate = startDate,
                endDate = endDate
            )
        }
    }
    fun renameJourney(id: Int, title: String) {
        updateJourney(id) {
            copy(title = title)
        }
    }

    // pin management
    fun addPin(journeyId: Int, location: JourneyLocation) {
        updateJourney(journeyId) {
            copy(locations = locations + location)
        }
    }

    fun removePin(journeyId: Int, locationId: Int) {
        updateJourney(journeyId) {
            copy(locations = locations.filter { it.id != locationId })
        }
    }
    fun updatePins(journeyId: Int, locations: List<JourneyLocation>) {
        updateJourney(journeyId) {
            copy(locations = locations)
        }
    }

}