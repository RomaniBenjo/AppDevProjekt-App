package com.example.commingsoon.viewmodels

import android.content.Context
import android.util.Xml
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asComposePath
import androidx.core.graphics.PathParser
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
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

    var countries by mutableStateOf<List<MapCountry>>(emptyList())
        private set

    fun loadWorldMap(context: Context) {
        if (countries.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = mutableListOf<MapCountry>()
                context.assets.open("world.svg").use { inputStream ->
                    val parser = Xml.newPullParser()
                    parser.setInput(inputStream, null)
                    var eventType = parser.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG && parser.name == "path") {
                            val id = parser.getAttributeValue(null, "id")
                                ?: parser.getAttributeValue(null, "class")
                            val d = parser.getAttributeValue(null, "d")
                            if (id != null && d != null) {
                                try {
                                    val androidPath = PathParser.createPathFromPathData(d)
                                    val composePath = androidPath.asComposePath()
                                    list.add(MapCountry(id, composePath))
                                } catch (e: Exception) {
                                    // Ignore malformed paths
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                }
                withContext(Dispatchers.Main) {
                    countries = list
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    init {
        _journeys.addAll(JourneyPlaceholder.journeys)
    }
    fun getNextJourneyId(): Int {
        return (_journeys.maxOfOrNull { it.id } ?: 0) + 1
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
    fun updateJourney(updatedJourney: Journey) {
        val index = _journeys.indexOfFirst { it.id == updatedJourney.id }
        if (index != -1) {
            _journeys[index] = updatedJourney
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