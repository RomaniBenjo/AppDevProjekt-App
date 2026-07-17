package com.example.commingsoon.viewmodels

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Xml
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asComposePath
import androidx.core.graphics.PathParser
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.commingsoon.db.AppDatabase
import com.example.commingsoon.db.JourneyDao
import com.example.commingsoon.db.JourneyEntity
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
    val locations: List<JourneyLocation>,
    val visitedCountries: List<String> = emptyList()
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
                            val name = parser.getAttributeValue(null, "name")
                            val d = parser.getAttributeValue(null, "d")
                            if (id != null && d != null) {
                                try {
                                    val androidPath = PathParser.createPathFromPathData(d)
                                    val composePath = androidPath.asComposePath()
                                    list.add(MapCountry(id, name, composePath))
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


    private var database: AppDatabase? = null
    private val dao: JourneyDao?
        get() = database?.journeyDao()

    var isNetworkAvailable by mutableStateOf(false)
        private set

    var isSyncing by mutableStateOf(false)
        private set

    private var isLoaded = false

    fun loadJourneys(context: Context) {
        if (isLoaded) return
        isLoaded = true
        
        val appContext = context.applicationContext
        database = AppDatabase.getDatabase(appContext)
        
        isNetworkAvailable = isOnline(appContext)
        registerNetworkCallback(appContext)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentDao = dao ?: return@launch
                var list = currentDao.getAllJourneys()
                if (list.isEmpty()) {
                    JourneyPlaceholder.journeys.forEach { journey ->
                        currentDao.insert(JourneyEntity.fromDomain(journey, pendingSync = false, isSynced = true))
                    }
                    list = currentDao.getAllJourneys()
                }
                withContext(Dispatchers.Main) {
                    _journeys.clear()
                    _journeys.addAll(list.map { it.toDomain() })
                    triggerSync()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun registerNetworkCallback(context: Context) {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    isNetworkAvailable = true
                    triggerSync()
                }
                override fun onLost(network: Network) {
                    isNetworkAvailable = false
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun triggerSync() {
        if (!isNetworkAvailable || isSyncing) return
        val currentDao = dao ?: return
        isSyncing = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val unsynced = currentDao.getUnsyncedJourneys()
                if (unsynced.isNotEmpty()) {
                    // Simulate network sync delay
                    kotlinx.coroutines.delay(2000)
                    unsynced.forEach { entity ->
                        val syncedEntity = entity.copy(isSynced = true, pendingSync = false)
                        currentDao.update(syncedEntity)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) {
                    isSyncing = false
                }
            }
        }
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
        viewModelScope.launch(Dispatchers.IO) {
            dao?.insert(JourneyEntity.fromDomain(journey, pendingSync = true, isSynced = false))
            triggerSync()
        }
    }

    fun removeJourney(id: Int) {
        _journeys.removeIf { it.id == id }
        viewModelScope.launch(Dispatchers.IO) {
            dao?.deleteById(id)
        }
    }

    private fun updateJourney(
        id: Int,
        update: Journey.() -> Journey
    ) {
        val index = _journeys.indexOfFirst { it.id == id }
        if (index != -1) {
            val updated = _journeys[index].update()
            _journeys[index] = updated
            viewModelScope.launch(Dispatchers.IO) {
                dao?.insert(JourneyEntity.fromDomain(updated, pendingSync = true, isSynced = false))
                triggerSync()
            }
        }
    }

    fun updateJourney(updatedJourney: Journey) {
        val index = _journeys.indexOfFirst { it.id == updatedJourney.id }
        if (index != -1) {
            _journeys[index] = updatedJourney
            viewModelScope.launch(Dispatchers.IO) {
                dao?.insert(JourneyEntity.fromDomain(updatedJourney, pendingSync = true, isSynced = false))
                triggerSync()
            }
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