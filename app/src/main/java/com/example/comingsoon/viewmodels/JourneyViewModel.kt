package com.example.comingsoon.viewmodels

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
import android.location.Geocoder
import android.location.Address
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import java.util.Locale
import com.example.comingsoon.db.AppDatabase
import com.example.comingsoon.db.JourneyDao
import com.example.comingsoon.db.JourneyEntity
import com.example.comingsoon.db.ClaimedCountryDao
import com.example.comingsoon.db.ClaimedCountryEntity
import com.example.comingsoon.sync.ClaimedCountriesRepository
import com.example.comingsoon.sync.JourneysRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
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

sealed interface ClaimStatus {
    object Idle : ClaimStatus
    object Detecting : ClaimStatus
    data class Success(val countryName: String, val isNew: Boolean) : ClaimStatus
    data class Error(val message: String) : ClaimStatus
}

class JourneyViewModel(
    private val journeysRepository: JourneysRepository,
    private val claimedCountriesRepository: ClaimedCountriesRepository
) : ViewModel() {
    private val _journeys = mutableStateListOf<Journey>()

    val journeys: List<Journey>
        get() = _journeys

    private val _claimedCountries = mutableStateListOf<String>()
    val claimedCountries: List<String>
        get() = _claimedCountries

    var claimStatus by mutableStateOf<ClaimStatus>(ClaimStatus.Idle)
        private set

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
                                ?: parser.getAttributeValue(null, "class")
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
    private val claimedDao: ClaimedCountryDao?
        get() = database?.claimedCountryDao()

    var isNetworkAvailable by mutableStateOf(false)
        private set

    var isSyncing by mutableStateOf(false)
        private set

    private val syncMutex = Mutex()

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
                val list = currentDao.getAllJourneys()
                val dbClaimedDao = claimedDao
                val claims = dbClaimedDao?.getAllClaims() ?: emptyList()
                withContext(Dispatchers.Main) {
                    _journeys.clear()
                    _journeys.addAll(list.map { it.toDomain() })
                    _claimedCountries.clear()
                    _claimedCountries.addAll(claims.map { it.id })
                    triggerSync()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetClaimStatus() {
        claimStatus = ClaimStatus.Idle
    }

    fun claimCurrentCountry(context: Context, latitudeOverride: Double? = null, longitudeOverride: Double? = null) {
        claimStatus = ClaimStatus.Detecting
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lat: Double
                val lng: Double

                if (latitudeOverride != null && longitudeOverride != null) {
                    lat = latitudeOverride
                    lng = longitudeOverride
                } else {
                    if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        withContext(Dispatchers.Main) {
                            claimStatus = ClaimStatus.Error("Standortberechtigung nicht erteilt.")
                        }
                        return@launch
                    }

                    val fusedClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                    val tag = "JourneyViewModel"

                    // Fast path: a recent cached fix, same source Google Maps relies on.
                    val cachedLocation = kotlinx.coroutines.suspendCancellableCoroutine<Location?> { continuation ->
                        try {
                            fusedClient.lastLocation
                                .addOnSuccessListener { loc ->
                                    if (continuation.isActive) continuation.resume(loc, onCancellation = null)
                                }
                                .addOnFailureListener { e ->
                                    android.util.Log.e(tag, "lastLocation failed", e)
                                    if (continuation.isActive) continuation.resume(null, onCancellation = null)
                                }
                        } catch (e: SecurityException) {
                            if (continuation.isActive) continuation.resume(null, onCancellation = null)
                        }
                    }

                    val location = cachedLocation ?: kotlinx.coroutines.withTimeoutOrNull(20_000L) {
                        kotlinx.coroutines.suspendCancellableCoroutine<Location?> { continuation ->
                            val request = com.google.android.gms.location.LocationRequest.Builder(
                                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 1000L
                            ).setMaxUpdates(1).build()

                            val callback = object : com.google.android.gms.location.LocationCallback() {
                                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                                    fusedClient.removeLocationUpdates(this)
                                    if (continuation.isActive) continuation.resume(result.lastLocation, onCancellation = null)
                                }
                            }

                            try {
                                fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
                                continuation.invokeOnCancellation {
                                    fusedClient.removeLocationUpdates(callback)
                                }
                            } catch (e: SecurityException) {
                                if (continuation.isActive) continuation.resume(null, onCancellation = null)
                            }
                        }
                    }

                    if (location != null) {
                        lat = location.latitude
                        lng = location.longitude
                    } else {
                        withContext(Dispatchers.Main) {
                            claimStatus = ClaimStatus.Error("Standort konnte nicht ermittelt werden. Bitte stelle sicher, dass Standortdienste aktiviert sind und versuche es im Freien erneut.")
                        }
                        return@launch
                    }
                }

                // English locale keeps countryName consistent with the English country
                // names used as fallback ids/classes in world.svg for countries without
                // an ISO id (e.g. "United States", "Russian Federation").
                val geocoder = Geocoder(context, Locale.ENGLISH)
                val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    var list: List<Address>? = null
                    val latch = java.util.concurrent.CountDownLatch(1)
                    geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addrList: List<Address>) {
                            list = addrList
                            latch.countDown()
                        }
                        override fun onError(errorMessage: String?) {
                            latch.countDown()
                        }
                    })
                    latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
                    list
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lng, 1)
                }

                val countryName = addresses?.firstOrNull()?.countryName
                val countryCode = addresses?.firstOrNull()?.countryCode

                if (countryCode == null || countryName == null) {
                    withContext(Dispatchers.Main) {
                        claimStatus = ClaimStatus.Error("Land für Koordinaten ($lat, $lng) nicht erkannt.")
                    }
                    return@launch
                }

                fun normalize(s: String) = s.lowercase(Locale.ENGLISH)
                    .replace("the ", "")
                    .replace(Regex("[^a-z]"), "")

                val normalizedCountryName = normalize(countryName)
                val matchedCountry = countries.find { country ->
                    country.id.equals(countryCode, ignoreCase = true) ||
                    country.name?.equals(countryName, ignoreCase = true) == true ||
                    country.name?.let { normalize(it) }?.let { normalizedName ->
                        normalizedName == normalizedCountryName ||
                        normalizedName.contains(normalizedCountryName) ||
                        normalizedCountryName.contains(normalizedName)
                    } == true
                }

                if (matchedCountry == null) {
                    withContext(Dispatchers.Main) {
                        claimStatus = ClaimStatus.Error("Erkanntes Land ($countryName, Code: $countryCode) ist auf der Karte nicht verfügbar.")
                    }
                    return@launch
                }

                val svgId = matchedCountry.id
                val dbClaimedDao = claimedDao
                val allClaims = dbClaimedDao?.getAllClaims() ?: emptyList()
                val alreadyClaimed = allClaims.any { it.id.equals(svgId, ignoreCase = true) }

                if (!alreadyClaimed) {
                    dbClaimedDao?.insertClaim(
                        ClaimedCountryEntity(
                            id = svgId,
                            name = matchedCountry.name ?: countryName,
                            claimedAt = System.currentTimeMillis(),
                            pendingSync = true
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    if (!alreadyClaimed) {
                        _claimedCountries.add(svgId)
                    }
                    claimStatus = ClaimStatus.Success(matchedCountry.name ?: countryName, !alreadyClaimed)
                }
                if (!alreadyClaimed) triggerSync()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    claimStatus = ClaimStatus.Error("Fehler beim Claimen: ${e.localizedMessage}")
                }
            }
        }
    }

    fun clearAllClaims() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                claimedDao?.deleteAllClaims()
                claimedCountriesRepository.markClearAllPending()
                withContext(Dispatchers.Main) {
                    _claimedCountries.clear()
                    claimStatus = ClaimStatus.Idle
                }
                triggerSync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isOnline(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
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
        if (!isNetworkAvailable) return
        if (!journeysRepository.hasSession()) return
        val currentDao = dao ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // tryLock (not a plain isSyncing check-then-set) so two near-simultaneous
            // callers can't both slip past the guard and push the same unsynced
            // journey twice, creating a duplicate on the server.
            if (!syncMutex.tryLock()) return@launch
            try {
                withContext(Dispatchers.Main) { isSyncing = true }
                journeysRepository.synchronize()
                claimedCountriesRepository.synchronize()
                val refreshedJourneys = currentDao.getAllJourneys().map { it.toDomain() }
                val refreshedClaims = claimedDao?.getAllClaims()?.map { it.id } ?: emptyList()
                withContext(Dispatchers.Main) {
                    _journeys.clear()
                    _journeys.addAll(refreshedJourneys)
                    _claimedCountries.clear()
                    _claimedCountries.addAll(refreshedClaims)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                syncMutex.unlock()
                withContext(Dispatchers.Main) {
                    isSyncing = false
                }
            }
        }
    }

    /** Pushes any pre-login local data and pulls the account's server state. */
    fun onSignedIn() {
        triggerSync()
    }

    /** Local journeys/claims stay visible; syncing simply stops until the next sign-in. */
    fun onSignedOut() {
        isSyncing = false
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
            val entity = dao?.getJourneyById(id)
            if (entity?.serverId == null) {
                dao?.deleteById(id)
            } else {
                dao?.update(entity.copy(deletedLocally = true, pendingSync = true))
            }
            triggerSync()
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
                val existing = dao?.getJourneyById(id)
                dao?.insert(
                    JourneyEntity.fromDomain(
                        updated, pendingSync = true, isSynced = false, serverId = existing?.serverId
                    )
                )
                triggerSync()
            }
        }
    }

    fun updateJourney(updatedJourney: Journey) {
        val index = _journeys.indexOfFirst { it.id == updatedJourney.id }
        if (index != -1) {
            _journeys[index] = updatedJourney
            viewModelScope.launch(Dispatchers.IO) {
                val existing = dao?.getJourneyById(updatedJourney.id)
                dao?.insert(
                    JourneyEntity.fromDomain(
                        updatedJourney, pendingSync = true, isSynced = false, serverId = existing?.serverId
                    )
                )
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