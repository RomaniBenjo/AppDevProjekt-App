package com.example.comingsoon.viewmodels

import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Xml
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asComposePath
import androidx.core.app.ActivityCompat
import androidx.core.graphics.PathParser
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comingsoon.R
import com.example.comingsoon.db.AppDatabase
import com.example.comingsoon.db.ClaimedCountryEntity
import com.example.comingsoon.errors.localizedUserMessage
import com.example.comingsoon.language.localizedCountryName
import com.example.comingsoon.language.localizedString
import com.example.comingsoon.language.persistedAppLanguage
import com.example.comingsoon.sync.ClaimedCountriesRepository
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.xmlpull.v1.XmlPullParser
import kotlin.coroutines.resume

sealed interface ClaimStatus {
    data object Idle : ClaimStatus
    data object Detecting : ClaimStatus
    data class Success(val countryName: String, val isNew: Boolean) : ClaimStatus
    data class Error(val message: String) : ClaimStatus
}

class CountryViewModel(
    private val repository: ClaimedCountriesRepository,
    context: Context
) : ViewModel() {
    private val appContext = context.applicationContext
    private val dao = AppDatabase.getDatabase(appContext).claimedCountryDao()
    private val _claimedCountries = mutableStateListOf<String>()
    val claimedCountries: List<String> get() = _claimedCountries

    var countries by mutableStateOf<List<MapCountry>>(emptyList())
        private set
    var claimStatus by mutableStateOf<ClaimStatus>(ClaimStatus.Idle)
        private set
    var syncError by mutableStateOf<String?>(null)
        private set
    private var loaded = false
    private val syncMutex = Mutex()

    fun load() {
        if (loaded) return
        loaded = true
        loadWorldMap()
        viewModelScope.launch(Dispatchers.IO) {
            val claims = dao.getAllClaims().map { it.id }
            withContext(Dispatchers.Main) {
                _claimedCountries.replaceWith(claims)
            }
        }
    }

    fun loadWorldMap() {
        if (countries.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val parsed = runCatching {
                buildList {
                    appContext.assets.open("world.svg").use { input ->
                        val parser = Xml.newPullParser().apply { setInput(input, null) }
                        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "path") {
                                val id = parser.getAttributeValue(null, "id")
                                    ?: parser.getAttributeValue(null, "class")
                                val name = parser.getAttributeValue(null, "name")
                                    ?: parser.getAttributeValue(null, "class")
                                val path = parser.getAttributeValue(null, "d")
                                if (id != null && path != null) {
                                    runCatching {
                                        add(
                                            MapCountry(
                                                id,
                                                name,
                                                PathParser.createPathFromPathData(path).asComposePath()
                                            )
                                        )
                                    }
                                }
                            }
                            parser.next()
                        }
                    }
                }
            }.getOrElse { emptyList() }
            withContext(Dispatchers.Main) { countries = parsed }
        }
    }

    fun resetClaimStatus() {
        claimStatus = ClaimStatus.Idle
    }

    fun onLanguageChanged() {
        claimStatus = ClaimStatus.Idle
        syncError = null
    }

    fun claimCurrentCountry(
        context: Context,
        latitudeOverride: Double? = null,
        longitudeOverride: Double? = null
    ) {
        claimStatus = ClaimStatus.Detecting
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val location = if (latitudeOverride != null && longitudeOverride != null) {
                    Location("").apply {
                        latitude = latitudeOverride
                        longitude = longitudeOverride
                    }
                } else {
                    currentLocation(context) ?: run {
                        withContext(Dispatchers.Main) {
                            claimStatus = ClaimStatus.Error(
                                appContext.localizedString(R.string.claim_location_unavailable)
                            )
                        }
                        return@launch
                    }
                }
                val address = reverseGeocode(context, location.latitude, location.longitude)
                val countryCode = address?.countryCode
                val countryName = address?.countryName
                if (countryCode == null || countryName == null) {
                    withContext(Dispatchers.Main) {
                        claimStatus = ClaimStatus.Error(
                            appContext.localizedString(
                                R.string.claim_country_not_recognized,
                                location.latitude,
                                location.longitude
                            )
                        )
                    }
                    return@launch
                }
                val matched = matchCountry(countryCode, countryName)
                if (matched == null) {
                    withContext(Dispatchers.Main) {
                        claimStatus = ClaimStatus.Error(
                            appContext.localizedString(
                                R.string.claim_country_not_on_map,
                                countryName,
                                countryCode
                            )
                        )
                    }
                    return@launch
                }
                val existing = dao.getAllClaims().any { it.id.equals(matched.id, true) }
                if (!existing) {
                    dao.insertClaim(
                        ClaimedCountryEntity(
                            matched.id,
                            matched.name ?: countryName,
                            System.currentTimeMillis(),
                            pendingSync = true
                        )
                    )
                }
                withContext(Dispatchers.Main) {
                    if (!existing) _claimedCountries.add(matched.id)
                    val locale = Locale.forLanguageTag(
                        appContext.persistedAppLanguage().languageTag
                    )
                    claimStatus = ClaimStatus.Success(
                        localizedCountryName(
                            countryCode,
                            matched.name ?: countryName,
                            locale
                        ),
                        !existing
                    )
                }
                if (!existing) synchronize()
            } catch (exception: SecurityException) {
                withContext(Dispatchers.Main) {
                    claimStatus = ClaimStatus.Error(
                        appContext.localizedString(R.string.claim_permission_denied)
                    )
                }
            } catch (exception: Exception) {
                val reason = exception.localizedUserMessage(appContext, R.string.unknown_error)
                withContext(Dispatchers.Main) {
                    claimStatus = ClaimStatus.Error(
                        appContext.localizedString(R.string.claim_failed, reason)
                    )
                }
            }
        }
    }

    fun clearAllClaims() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                dao.deleteAllClaims()
                repository.markClearAllPending()
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    _claimedCountries.clear()
                    claimStatus = ClaimStatus.Idle
                }
                synchronize()
            }.onFailure {
                withContext(Dispatchers.Main) {
                    syncError = it.localizedUserMessage(appContext, R.string.claim_sync_failed)
                }
            }
        }
    }

    fun synchronize() {
        if (!repository.hasSession()) return
        viewModelScope.launch(Dispatchers.IO) {
            if (!syncMutex.tryLock()) return@launch
            try {
                val result = runCatching { repository.synchronize() }
                val claims = dao.getAllClaims().map { it.id }
                withContext(Dispatchers.Main) {
                    _claimedCountries.replaceWith(claims)
                    syncError = result.exceptionOrNull()?.localizedUserMessage(
                        appContext,
                        R.string.claim_sync_failed
                    )
                }
            } finally {
                syncMutex.unlock()
            }
        }
    }

    private suspend fun currentLocation(context: Context): Location? {
        if (
            ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Location permission missing")
        }
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cached = suspendCancellableCoroutine<Location?> { continuation ->
            client.lastLocation
                .addOnSuccessListener {
                    if (continuation.isActive) continuation.resume(it)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
        }
        return cached ?: withTimeoutOrNull(20_000L) {
            suspendCancellableCoroutine { continuation ->
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
                    .setMaxUpdates(1)
                    .build()
                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        client.removeLocationUpdates(this)
                        if (continuation.isActive) {
                            continuation.resume(result.lastLocation)
                        }
                    }
                }
                client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                continuation.invokeOnCancellation { client.removeLocationUpdates(callback) }
            }
        }
    }

    private fun reverseGeocode(context: Context, latitude: Double, longitude: Double): Address? {
        val geocoder = Geocoder(context, Locale.ENGLISH)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION")
            return geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
        }
        var addresses: List<Address>? = null
        val latch = CountDownLatch(1)
        geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
            override fun onGeocode(results: List<Address>) {
                addresses = results
                latch.countDown()
            }
            override fun onError(errorMessage: String?) {
                latch.countDown()
            }
        })
        latch.await(5, TimeUnit.SECONDS)
        return addresses?.firstOrNull()
    }

    private fun matchCountry(code: String, name: String): MapCountry? {
        fun normalize(value: String) = value.lowercase(Locale.ENGLISH)
            .replace("the ", "")
            .replace(Regex("[^a-z]"), "")
        val normalized = normalize(name)
        return countries.find { country ->
            country.id.equals(code, true) ||
                country.name?.equals(name, true) == true ||
                country.name?.let(::normalize)?.let {
                    it == normalized || it.contains(normalized) || normalized.contains(it)
                } == true
        }
    }

    private fun <T> MutableList<T>.replaceWith(values: List<T>) {
        clear()
        addAll(values)
    }
}
