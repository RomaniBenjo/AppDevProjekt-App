package com.example.comingsoon.viewmodels

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comingsoon.R
import com.example.comingsoon.db.AppDatabase
import com.example.comingsoon.errors.localizedUserMessage
import com.example.comingsoon.friends.OfflineFriendIdentity
import com.example.comingsoon.friends.OfflineJourneyPayload
import com.example.comingsoon.language.localizedString
import com.example.comingsoon.sync.JourneyShareSnapshot
import com.example.comingsoon.sync.JourneysRepository
import com.example.comingsoon.sync.PendingJourneyShare
import com.example.comingsoon.sync.PendingJourneyShareAction
import com.example.comingsoon.sync.effectiveJourneyShareType
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class JourneyShareViewModel(
    private val repository: JourneysRepository,
    context: Context
) : ViewModel() {
    private val appContext = context.applicationContext
    private val journeyDao = AppDatabase.getDatabase(appContext).journeyDao()
    private val _shares = mutableStateListOf<JourneyShareSnapshot>()
    private val _pendingShares = mutableStateListOf<PendingJourneyShare>()
    private val operationMutex = Mutex()
    private var refreshJourneys: suspend () -> Unit = {}

    var error by mutableStateOf<String?>(null)
        private set
    var syncError by mutableStateOf<String?>(null)
        private set
    var feedback by mutableStateOf<String?>(null)
        private set
    var operationKey by mutableStateOf<String?>(null)
        private set
    var qrDeepLink by mutableStateOf<String?>(null)
        private set
    var qrExpiresAt by mutableStateOf<String?>(null)
        private set
    var isCreatingQrLink by mutableStateOf(false)
        private set

    fun bindJourneyRefresh(callback: suspend () -> Unit) {
        refreshJourneys = callback
    }

    fun load() {
        viewModelScope.launch(Dispatchers.IO) { refreshCachedState() }
    }

    fun onLanguageChanged() {
        error = null
        syncError = null
        feedback = null
    }

    fun onSignedIn() = synchronize()

    fun onSignedOut() {
        error = null
        syncError = null
        feedback = null
        operationKey = null
        qrDeepLink = null
        qrExpiresAt = null
        _shares.clear()
        _pendingShares.clear()
    }

    fun getSharedJourney(ownerId: Int, serverJourneyId: Int): Journey? =
        _shares.firstOrNull {
            it.ownerId == ownerId && it.journey.serverId == serverJourneyId
        }?.journey

    fun shareTypeFor(journey: Journey, friendId: Int): String? =
        effectiveJourneyShareType(
            remoteShareType = shareFor(journey, friendId)?.shareType,
            pendingAction = _pendingShares.firstOrNull {
                it.localJourneyId == journey.id && it.recipientId == friendId
            }?.action
        )

    fun sharesByMeWith(friendId: Int, journeys: List<Journey>): List<JourneyShareSnapshot> {
        val pendingUnshares = _pendingShares
            .filter {
                it.recipientId == friendId && it.action == PendingJourneyShareAction.UNSHARE
            }
            .mapTo(mutableSetOf(), PendingJourneyShare::localJourneyId)
        val result = _shares.filter { share ->
            if (share.recipientId != friendId) return@filter false
            val localId = journeys.firstOrNull {
                it.serverId == share.journey.serverId
            }?.id
            localId == null || localId !in pendingUnshares
        }.toMutableList()
        val remoteIds = result.mapNotNullTo(mutableSetOf()) { it.journey.serverId }
        val owner = repository.currentUser()
        _pendingShares
            .filter {
                it.recipientId == friendId && it.action == PendingJourneyShareAction.SHARE
            }
            .forEach { pending ->
                val journey = journeys.firstOrNull { it.id == pending.localJourneyId }
                    ?: return@forEach
                if (journey.serverId != null && journey.serverId in remoteIds) return@forEach
                result += JourneyShareSnapshot(
                    ownerId = pending.ownerId,
                    recipientId = pending.recipientId,
                    ownerName = owner?.name?.takeIf(String::isNotBlank)
                        ?: owner?.email?.substringBefore('@')
                        ?: appContext.localizedString(R.string.you),
                    ownerEmail = owner?.email.orEmpty(),
                    ownerPictureUrl = owner?.pictureUrl,
                    shareType = "manual",
                    sharedAt = Instant.ofEpochMilli(pending.createdAtEpochMillis).toString(),
                    journey = journey,
                    localJourneyId = journey.id
                )
            }
        return result.sortedByDescending { it.journey.startDate }
    }

    fun sharesWithMeBy(friendId: Int): List<JourneyShareSnapshot> =
        _shares.filter { it.ownerId == friendId }

    suspend fun receiveOfflineJourney(
        owner: OfflineFriendIdentity,
        payload: OfflineJourneyPayload
    ) {
        repository.storeReceivedOfflineShare(owner, payload)
        refreshCachedState()
        withContext(Dispatchers.Main) {
            feedback = appContext.localizedString(
                R.string.offline_journey_received,
                payload.title,
                owner.name
            )
        }
    }

    suspend fun recordSentOfflineJourney(
        recipient: OfflineFriendIdentity,
        payload: OfflineJourneyPayload
    ) {
        repository.storeSentOfflineShare(recipient, payload)
        refreshCachedState()
        withContext(Dispatchers.Main) {
            feedback = appContext.localizedString(
                R.string.offline_journey_shared,
                payload.title,
                recipient.name
            )
        }
    }

    fun shareJourney(journeyId: Int, friendId: Int) =
        changeShare(journeyId, friendId, PendingJourneyShareAction.SHARE)

    fun unshareJourney(journeyId: Int, friendId: Int) =
        changeShare(journeyId, friendId, PendingJourneyShareAction.UNSHARE)

    private fun changeShare(
        journeyId: Int,
        friendId: Int,
        action: PendingJourneyShareAction
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            operationMutex.withLock {
                withContext(Dispatchers.Main) {
                    operationKey = "$journeyId:$friendId"
                    error = null
                    feedback = null
                }
                try {
                    val online = isOnline() && repository.hasSession()
                    when {
                        action == PendingJourneyShareAction.SHARE && online ->
                            repository.shareJourney(journeyId, friendId)
                        action == PendingJourneyShareAction.SHARE ->
                            repository.queueShare(journeyId, friendId)
                        online -> repository.unshareJourney(journeyId, friendId)
                        else -> repository.queueUnshare(journeyId, friendId)
                    }
                    refreshJourneys()
                    val remote = if (online) {
                        runCatching { repository.refreshJourneyShares() }.getOrNull()
                    } else {
                        null
                    }
                    val pending = repository.loadPendingShares()
                    val remainsPending = pending.any {
                        it.localJourneyId == journeyId &&
                            it.recipientId == friendId &&
                            it.action == action
                    }
                    withContext(Dispatchers.Main) {
                        remote?.let { _shares.replaceWith(it) }
                        _pendingShares.replaceWith(pending)
                        feedback = when (action) {
                            PendingJourneyShareAction.SHARE -> appContext.localizedString(
                                if (remainsPending) R.string.share_queued else R.string.share_succeeded
                            )
                            PendingJourneyShareAction.UNSHARE -> appContext.localizedString(
                                if (remainsPending) R.string.unshare_queued else R.string.unshare_succeeded
                            )
                        }
                    }
                } catch (exception: Exception) {
                    withContext(Dispatchers.Main) {
                        error = exception.localizedUserMessage(
                            appContext,
                            if (action == PendingJourneyShareAction.SHARE) {
                                R.string.share_failed
                            } else {
                                R.string.unshare_failed
                            }
                        )
                    }
                } finally {
                    withContext(Dispatchers.Main) { operationKey = null }
                }
            }
        }
    }

    fun createQrShareLink(journeyId: Int) {
        if (isCreatingQrLink || qrDeepLink != null) return
        if (!isOnline() || !repository.hasSession()) {
            error = appContext.localizedString(R.string.qr_requires_connection)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    isCreatingQrLink = true
                    error = null
                }
                val link = repository.createShareLink(journeyId)
                refreshJourneys()
                withContext(Dispatchers.Main) {
                    qrDeepLink = link.deepLink
                    qrExpiresAt = link.expiresAt
                }
            } catch (exception: Exception) {
                withContext(Dispatchers.Main) {
                    error = exception.localizedUserMessage(appContext, R.string.qr_create_failed)
                }
            } finally {
                withContext(Dispatchers.Main) { isCreatingQrLink = false }
            }
        }
    }

    fun acceptShareLink(token: String, onResult: (JourneyShareSnapshot?) -> Unit) {
        if (!repository.hasSession()) {
            error = appContext.localizedString(R.string.shared_journey_sign_in)
            onResult(null)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val accepted = repository.acceptShareLink(token)
                refreshCachedState()
                withContext(Dispatchers.Main) {
                    feedback = appContext.localizedString(R.string.shared_journey_added)
                    onResult(accepted)
                }
            } catch (exception: Exception) {
                withContext(Dispatchers.Main) {
                    error = exception.localizedUserMessage(
                        appContext,
                        R.string.share_link_open_failed,
                        R.string.share_link_already_used
                    )
                    onResult(null)
                }
            }
        }
    }

    fun resetQrShareLink() {
        qrDeepLink = null
        qrExpiresAt = null
        error = null
    }

    fun clearMessage() {
        error = null
        feedback = null
    }

    fun synchronize() {
        if (!repository.hasSession() || !isOnline()) return
        viewModelScope.launch(Dispatchers.IO) {
            operationMutex.withLock {
                val result = runCatching {
                    repository.synchronize()
                    repository.refreshJourneyShares()
                }
                refreshJourneys()
                val pending = repository.loadPendingShares()
                withContext(Dispatchers.Main) {
                    result.getOrNull()?.let { _shares.replaceWith(it) }
                    _pendingShares.replaceWith(pending)
                    syncError = result.exceptionOrNull()?.localizedUserMessage(
                        appContext,
                        R.string.share_sync_failed
                    )
                }
            }
        }
    }

    private fun shareFor(journey: Journey, friendId: Int): JourneyShareSnapshot? {
        val ownerId = repository.currentUserId() ?: return null
        return _shares.firstOrNull {
            it.ownerId == ownerId &&
                it.recipientId == friendId &&
                (
                    it.localJourneyId == journey.id ||
                        journey.serverId != null && it.journey.serverId == journey.serverId
                )
        }
    }

    private suspend fun refreshCachedState() {
        val shares = repository.loadCachedJourneyShares()
        val pending = repository.loadPendingShares()
        withContext(Dispatchers.Main) {
            _shares.replaceWith(shares)
            _pendingShares.replaceWith(pending)
        }
    }

    private fun isOnline(): Boolean {
        val manager = appContext.getSystemService(ConnectivityManager::class.java)
        val network = manager?.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun <T> MutableList<T>.replaceWith(values: List<T>) {
        clear()
        addAll(values)
    }
}
