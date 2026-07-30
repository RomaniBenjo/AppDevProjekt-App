package com.example.comingsoon.viewmodels

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comingsoon.R
import com.example.comingsoon.db.AppDatabase
import com.example.comingsoon.db.JourneyEntity
import com.example.comingsoon.errors.localizedUserMessage
import com.example.comingsoon.notifications.JourneyNotificationScheduler
import com.example.comingsoon.sync.JourneysRepository
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

data class Journey(
    val id: Int,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val shared: Boolean? = null,
    val locations: List<JourneyLocation>,
    val visitedCountries: List<String> = emptyList(),
    val serverId: Int? = null,
    val ownerId: Int? = null,
    val isOwned: Boolean = true
) {
    val pinCount: Int get() = locations.size
}

data class JourneyLocation(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

class JourneyViewModel(
    private val repository: JourneysRepository,
    context: Context
) : ViewModel() {
    private val appContext = context.applicationContext
    private val dao = AppDatabase.getDatabase(appContext).journeyDao()
    private val connectivityManager =
        appContext.getSystemService(ConnectivityManager::class.java)
    private val syncMutex = Mutex()
    private val _journeys = mutableStateListOf<Journey>()
    val journeys: List<Journey> get() = _journeys

    var isNetworkAvailable by mutableStateOf(isOnline())
        private set
    var isSyncing by mutableStateOf(false)
        private set
    var syncError by mutableStateOf<String?>(null)
        private set

    private var loaded = false
    private var remindersEnabled = false
    private var reminderTime = LocalTime.of(9, 0)
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun loadJourneys() {
        if (loaded) return
        loaded = true
        registerNetworkCallback()
        viewModelScope.launch(Dispatchers.IO) {
            reloadFromDatabase()
            withContext(Dispatchers.Main) {
                refreshAllReminders()
                triggerSync()
            }
        }
    }

    suspend fun reloadFromDatabase() {
        val values = dao.getAllJourneys().map(JourneyEntity::toDomain)
        withContext(Dispatchers.Main) {
            _journeys.replaceWith(values)
        }
    }

    fun triggerSync() {
        if (!isNetworkAvailable || !repository.hasSession()) return
        viewModelScope.launch(Dispatchers.IO) {
            if (!syncMutex.tryLock()) return@launch
            try {
                withContext(Dispatchers.Main) { isSyncing = true }
                val result = runCatching { repository.synchronize() }
                reloadFromDatabase()
                withContext(Dispatchers.Main) {
                    syncError = result.exceptionOrNull()?.localizedUserMessage(
                        appContext,
                        R.string.journey_sync_failed
                    )
                }
            } finally {
                syncMutex.unlock()
                withContext(Dispatchers.Main) { isSyncing = false }
            }
        }
    }

    fun onSignedIn() = triggerSync()

    fun onSignedOut() {
        isSyncing = false
        syncError = null
    }

    fun onLanguageChanged() {
        syncError = null
    }

    fun configureJourneyReminders(enabled: Boolean, reminderTime: LocalTime) {
        remindersEnabled = enabled
        this.reminderTime = reminderTime
        refreshAllReminders()
    }

    fun getNextJourneyId(): Int = (_journeys.maxOfOrNull(Journey::id) ?: 0) + 1

    fun getJourney(id: Int): Journey? = _journeys.find { it.id == id }

    fun addJourney(journey: Journey) {
        _journeys.add(journey)
        scheduleReminder(journey)
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(JourneyEntity.fromDomain(journey, pendingSync = true, isSynced = false))
            triggerSync()
        }
    }

    fun removeJourney(id: Int) {
        _journeys.removeAll { it.id == id }
        JourneyNotificationScheduler.cancel(appContext, id)
        viewModelScope.launch(Dispatchers.IO) {
            val entity = dao.getJourneyById(id)
            if (entity?.serverId == null) {
                dao.deleteById(id)
            } else {
                dao.update(entity.copy(deletedLocally = true, pendingSync = true))
            }
            triggerSync()
        }
    }

    fun updateJourney(journey: Journey) {
        val index = _journeys.indexOfFirst { it.id == journey.id }
        if (index < 0) return
        _journeys[index] = journey
        scheduleReminder(journey)
        viewModelScope.launch(Dispatchers.IO) {
            val existing = dao.getJourneyById(journey.id)
            dao.insert(
                JourneyEntity.fromDomain(
                    journey,
                    pendingSync = true,
                    isSynced = false,
                    serverId = existing?.serverId
                )
            )
            triggerSync()
        }
    }

    private fun refreshAllReminders() {
        if (!remindersEnabled) {
            JourneyNotificationScheduler.cancelAll(appContext)
        } else {
            _journeys.forEach(::scheduleReminder)
        }
    }

    private fun scheduleReminder(journey: Journey) {
        if (!remindersEnabled) {
            JourneyNotificationScheduler.cancel(appContext, journey.id)
            return
        }
        JourneyNotificationScheduler.scheduleJourneyReminder(
            appContext,
            journey.id,
            journey.title,
            journey.startDate.atTime(reminderTime)
        )
    }

    private fun isOnline(): Boolean {
        val network = connectivityManager?.activeNetwork ?: return false
        return connectivityManager.getNetworkCapabilities(network)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    private fun registerNetworkCallback() {
        if (networkCallback != null || connectivityManager == null) return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isNetworkAvailable = true
                triggerSync()
            }

            override fun onLost(network: Network) {
                isNetworkAvailable = isOnline()
            }
        }
        networkCallback = callback
        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build(),
            callback
        )
    }

    override fun onCleared() {
        networkCallback?.let {
            runCatching { connectivityManager?.unregisterNetworkCallback(it) }
        }
        networkCallback = null
        super.onCleared()
    }

    private fun <T> MutableList<T>.replaceWith(values: List<T>) {
        clear()
        addAll(values)
    }
}
