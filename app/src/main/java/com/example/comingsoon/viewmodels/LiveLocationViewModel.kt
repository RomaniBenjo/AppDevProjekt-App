package com.example.comingsoon.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comingsoon.BuildConfig
import com.example.comingsoon.friends.FriendsApiClient
import com.example.comingsoon.location.LiveLocationService
import com.example.comingsoon.sync.LiveLocationRepository
import com.example.comingsoon.sync.ServerFriendLiveLocation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLng
import com.example.comingsoon.R
import com.example.comingsoon.language.localizedString

class LiveLocationViewModel(
    private val repository: LiveLocationRepository,
    private val context: Context
) : ViewModel() {

    private val friendsApiClient = FriendsApiClient(BuildConfig.API_BASE_URL)

    var isSharing by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val _friendLocations = mutableStateListOf<ServerFriendLiveLocation>()
    val friendLocations: List<ServerFriendLiveLocation> get() = _friendLocations

    private val _selfTrail = mutableStateListOf<LatLng>()
    val selfTrail: List<LatLng> get() = _selfTrail
    var selfPosition by mutableStateOf<LatLng?>(null)
        private set
    var selfAccuracyMeters by mutableStateOf(0f)
        private set

    init {
        reconcileSharingState()
        refreshFriendLocations()
        startRealtimeUpdates()
        observeServiceState()
    }

    fun hasSession(): Boolean = repository.hasSession()

    fun startSharing() {
        _selfTrail.clear()
        LiveLocationService.start(context)
        isSharing = true
    }

    fun stopSharing() {
        LiveLocationService.stop(context)
        isSharing = false
        selfPosition = null
        _selfTrail.clear()
    }

    fun onLanguageChanged() {
        errorMessage = null
    }

    fun refreshFriendLocations() {
        viewModelScope.launch {
            runCatching { repository.getFriendsLiveLocations() }
                .onSuccess {
                    _friendLocations.clear()
                    _friendLocations.addAll(it)
                }
                .onFailure {
                    errorMessage = context.localizedString(R.string.live_locations_load_failed)
                }
        }
    }

    private fun reconcileSharingState() {
        if (!repository.hasSession()) return
        viewModelScope.launch {
            val serverActive = runCatching { repository.getSharingStatus().active }.getOrNull()
            when (serverActive) {
                true -> if (LiveLocationService.isRunning.value) {
                    isSharing = true
                } else {
                    // Server still thinks we're sharing but the local service died (e.g. a
                    // process kill) without ever calling the stop endpoint — resume it now
                    // that we have foreground context again, rather than leaving the account
                    // stuck "sharing" server-side with nothing actually pushing fixes.
                    _selfTrail.clear()
                    LiveLocationService.start(context)
                    isSharing = true
                }
                false -> isSharing = false
                null -> Unit
            }
        }
    }

    private fun observeServiceState() {
        viewModelScope.launch {
            LiveLocationService.isRunning.collect { running ->
                isSharing = running
            }
        }
        viewModelScope.launch {
            LiveLocationService.lastLocation.collect { location ->
                if (location == null) return@collect
                val point = LatLng(location.latitude, location.longitude)
                selfPosition = point
                selfAccuracyMeters = location.accuracy
                _selfTrail.add(point)
            }
        }
    }

    private var realtimeStarted = false
    private fun startRealtimeUpdates() {
        if (!repository.hasSession() || realtimeStarted) return
        realtimeStarted = true
        viewModelScope.launch {
            var retryDelayMillis = 1_000L
            while (isActive && repository.hasSession()) {
                try {
                    val token = repository.currentToken() ?: break
                    friendsApiClient.listenForFriendUpdates(token) {
                        refreshFriendLocations()
                    }
                    retryDelayMillis = 1_000L
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    // Reconnect quietly after temporary network interruptions.
                }
                delay(retryDelayMillis)
                retryDelayMillis = (retryDelayMillis * 2).coerceAtMost(30_000L)
            }
        }
    }
}
