package com.example.comingsoon.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comingsoon.auth.AuthenticatedUser
import com.example.comingsoon.friends.FriendsRepository
import com.example.comingsoon.friends.OfflineFriendEndpoint
import com.example.comingsoon.friends.OfflineFriendIdentity
import com.example.comingsoon.friends.OfflineJourneyPayload
import com.example.comingsoon.friends.OfflineFriendPairingManager
import com.example.comingsoon.friends.OfflinePairingPhase
import com.example.comingsoon.friends.ServerFriendRequest
import com.example.comingsoon.friends.StoredFriend
import com.example.comingsoon.friends.stableOfflineId
import com.example.comingsoon.friends.toOfflinePayload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import com.example.comingsoon.R
import com.example.comingsoon.language.localizedString
import com.example.comingsoon.notifications.NotificationsHelper
import com.example.comingsoon.notifications.FriendRequestNotificationStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class Friend(
    val id: Int,
    val name: String,
    val email: String = "",
    val image: Int? = null,
    val imageUrl: String? = null,
    val sharedWithMe: List<Journey> = emptyList(),
    val sharedByMe: List<Journey> = emptyList(),
    val liveLocation: FriendLocation? = null,
    val identityKey: String? = null,
    val addedNearby: Boolean = false,
    val isServerFriend: Boolean = true
)

data class FriendRequest(
    val id: Int,
    val user: Friend,
    val createdAt: String
)

data class FriendLocation(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Instant? = null
)

enum class FriendJourneyTab {
    SHARED_BY_ME,
    SHARED_WITH_ME
}

class FriendViewModel(
    private val repository: FriendsRepository,
    private val context: Context
) : ViewModel() {
    private var realtimeJob: Job? = null
    private val refreshMutex = Mutex()
    private val friendRequestNotificationStore =
        FriendRequestNotificationStore(context.applicationContext)
    private var onOfflineJourneyReceived: suspend (
        OfflineFriendIdentity,
        OfflineJourneyPayload
    ) -> Unit = { _, _ -> error("Offline journey receiver is not connected") }
    private var onOfflineJourneySent: suspend (
        OfflineFriendIdentity,
        OfflineJourneyPayload
    ) -> Unit = { _, _ -> error("Offline journey sender is not connected") }
    private val offlinePairingManager = OfflineFriendPairingManager(
        context = context.applicationContext,
        ownIdentity = repository::currentOfflineIdentity,
        onFriendReceived = { identity, pairingId ->
            repository.saveNearbyFriend(identity, pairingId)
            replaceFriendsFromCache()
        },
        onJourneyReceived = { owner, journey ->
            onOfflineJourneyReceived(owner, journey)
        },
        onJourneySent = { recipient, journey ->
            onOfflineJourneySent(recipient, journey)
        }
    )
    val offlinePairingState = offlinePairingManager.state

    private val _friends = mutableStateListOf<Friend>()
    val friends: List<Friend> get() = _friends

    private val _incomingRequests = mutableStateListOf<FriendRequest>()
    val incomingRequests: List<FriendRequest> get() = _incomingRequests

    private val _outgoingRequests = mutableStateListOf<FriendRequest>()
    val outgoingRequests: List<FriendRequest> get() = _outgoingRequests

    private val _searchResults = mutableStateListOf<Friend>()
    val searchResults: List<Friend> get() = _searchResults

    var isLoading by mutableStateOf(false)
        private set
    var isSearching by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var journeyShareUpdateVersion by mutableStateOf(0)
        private set

    val currentUserId: Int?
        get() = repository.currentUserId()

    init {
        loadCachedFriends()
        if (repository.hasSession()) {
            refresh()
            startRealtimeUpdates()
        }
    }

    fun getFriend(id: Int): Friend? = _friends.find { it.id == id }

    fun refresh() {
        refresh(showLoading = true, showErrors = true)
    }

    fun hasGooglePlayServices(): Boolean = offlinePairingManager.hasGooglePlayServices()
    fun hostOfflinePairing() = offlinePairingManager.startAdvertising()
    fun searchOfflineFriends() = offlinePairingManager.startDiscovery()
    fun connectOfflineFriend(endpoint: OfflineFriendEndpoint) =
        offlinePairingManager.requestConnection(endpoint)
    fun acceptOfflineFriend() = offlinePairingManager.acceptPendingConnection()
    fun rejectOfflineFriend() = offlinePairingManager.rejectPendingConnection()
    fun stopOfflinePairing() = offlinePairingManager.stop()

    fun bindOfflineJourneyCallbacks(
        onReceived: suspend (OfflineFriendIdentity, OfflineJourneyPayload) -> Unit,
        onSent: suspend (OfflineFriendIdentity, OfflineJourneyPayload) -> Unit
    ) {
        onOfflineJourneyReceived = onReceived
        onOfflineJourneySent = onSent
    }

    fun unbindOfflineJourneyCallbacks() {
        onOfflineJourneyReceived = { _, _ -> error("Offline journey receiver is not connected") }
        onOfflineJourneySent = { _, _ -> error("Offline journey sender is not connected") }
    }

    fun shareJourneyOffline(journey: Journey, friend: Friend) {
        val identityKey = friend.identityKey ?: return
        offlinePairingManager.startJourneyShare(
            targetIdentityKey = identityKey,
            targetFriendName = friend.name,
            journey = journey.toOfflinePayload()
        )
    }

    fun startRealtimeUpdates() {
        if (!repository.hasSession() || realtimeJob?.isActive == true) return
        realtimeJob = viewModelScope.launch {
            var retryDelayMillis = 1_000L
            while (isActive && repository.hasSession()) {
                try {
                    refreshNow(
                        showLoading = false,
                        showErrors = false,
                        notifyNewIncomingRequests = false
                    )
                    repository.listenForUpdates { eventType ->
                        viewModelScope.launch {
                            if (
                                eventType == "journey_shared" ||
                                eventType == "friends_changed"
                            ) {
                                journeyShareUpdateVersion += 1
                            }
                            refreshNow(
                                showLoading = false,
                                showErrors = false,
                                notifyNewIncomingRequests =
                                    eventType == "friend_request_created"
                            )
                        }
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

    fun onSignedOut() {
        realtimeJob?.cancel()
        realtimeJob = null
        _friends.clear()
        _incomingRequests.clear()
        _outgoingRequests.clear()
        _searchResults.clear()
        errorMessage = null
        isLoading = false
        isSearching = false
        journeyShareUpdateVersion = 0
    }

    private fun refresh(showLoading: Boolean, showErrors: Boolean) {
        viewModelScope.launch {
            refreshNow(showLoading, showErrors, notifyNewIncomingRequests = false)
        }
    }

    private suspend fun refreshNow(
        showLoading: Boolean,
        showErrors: Boolean,
        notifyNewIncomingRequests: Boolean
    ) = refreshMutex.withLock {
        if (showLoading) isLoading = true
        if (showErrors) errorMessage = null
        replaceFriendsFromCache()
        if (!repository.hasSession()) {
            if (showLoading) isLoading = false
            return@withLock
        }
        runCatching { repository.synchronizeFriends() }
            .onSuccess { snapshot ->
                applySnapshot(snapshot, notifyNewIncomingRequests)
            }.onFailure { throwable ->
                if (showErrors && _friends.isEmpty()) showError(throwable)
            }
        if (showLoading) isLoading = false
    }

    private fun applySnapshot(
        snapshot: com.example.comingsoon.friends.FriendsSyncResult,
        notifyNewIncomingRequests: Boolean
    ) {
        val incoming = snapshot.requests.incoming.map { it.toRequest(incoming = true) }
        val userId = currentUserId
        val newIncomingIds = when {
            userId == null -> emptySet()
            notifyNewIncomingRequests -> friendRequestNotificationStore
                .newRequests(userId, snapshot.requests.incoming)
                .mapTo(mutableSetOf(), ServerFriendRequest::id)
            else -> {
                friendRequestNotificationStore.remember(
                    userId,
                    snapshot.requests.incoming.map(ServerFriendRequest::id)
                )
                emptySet()
            }
        }
        _friends.replaceWith(snapshot.friends.map(::toFriend))
        _incomingRequests.replaceWith(incoming)
        _outgoingRequests.replaceWith(
            snapshot.requests.outgoing.map { it.toRequest(incoming = false) }
        )
        incoming.filter { it.id in newIncomingIds }.forEach { request ->
            NotificationsHelper(context).showFriendRequest(
                requestId = request.id,
                senderName = request.user.name
            )
        }
    }

    fun searchFriends(query: String) {
        val normalized = query.trim()
        if (normalized.length < 2) {
            _searchResults.clear()
            errorMessage = context.localizedString(R.string.friends_search_too_short)
            return
        }
        viewModelScope.launch {
            isSearching = true
            errorMessage = null
            runCatching { repository.searchUsers(normalized) }
                .onSuccess { users -> _searchResults.replaceWith(users.map(::toFriend)) }
                .onFailure(::showError)
            isSearching = false
        }
    }

    fun sendFriendRequest(friend: Friend) {
        sendFriendRequest(friend.id) {
            _searchResults.removeAll { it.id == friend.id }
        }
    }

    fun sendFriendRequest(userId: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            runCatching {
                repository.sendRequest(userId)
                refreshAfterMutation()
            }.onSuccess {
                onSuccess()
            }.onFailure(::showError)
            isLoading = false
        }
    }

    fun acceptRequest(requestId: Int) = mutate {
        repository.acceptRequest(requestId)
        refreshAfterMutation()
    }

    fun deleteRequest(requestId: Int) = mutate {
        repository.deleteRequest(requestId)
        refreshAfterMutation()
    }

    fun removeFriend(friend: Friend) = mutate {
        repository.markFriendDeleted(friend.toStoredFriend())
        replaceFriendsFromCache()
        if (repository.hasSession()) refreshAfterMutation()
    }

    fun clearError() {
        errorMessage = null
    }

    fun onLanguageChanged() {
        errorMessage = null
        if (offlinePairingState.value.phase == OfflinePairingPhase.ERROR) {
            offlinePairingManager.stop()
        }
    }

    private fun mutate(block: suspend () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            runCatching { block() }.onFailure(::showError)
            isLoading = false
        }
    }

    private suspend fun refreshAfterMutation() {
        val snapshot = repository.synchronizeFriends()
        applySnapshot(snapshot, notifyNewIncomingRequests = false)
    }

    private fun ServerFriendRequest.toRequest(incoming: Boolean) = FriendRequest(
        id = id,
        user = toFriend(if (incoming) sender else receiver),
        createdAt = createdAt
    )

    private fun toFriend(user: AuthenticatedUser) = Friend(
        id = user.id.toInt(),
        name = user.name?.takeIf { it.isNotBlank() } ?: user.email.substringBefore('@'),
        email = user.email,
        imageUrl = user.pictureUrl
    )

    private fun toFriend(friend: StoredFriend): Friend {
        val id = friend.serverUserId
            ?.takeIf { it in 1..Int.MAX_VALUE.toLong() }
            ?.toInt()
            ?: stableOfflineId(friend.identityKey)
        return Friend(
            id = id,
            name = friend.name,
            email = friend.email,
            imageUrl = friend.pictureUrl,
            identityKey = friend.identityKey,
            addedNearby = friend.addedNearby,
            isServerFriend = friend.isServerFriend
        )
    }

    private fun Friend.toStoredFriend() = StoredFriend(
        identityKey = requireNotNull(identityKey),
        serverUserId = id.takeIf { it > 0 }?.toLong(),
        name = name,
        email = email,
        pictureUrl = imageUrl,
        addedNearby = addedNearby,
        isServerFriend = isServerFriend
    )

    private fun loadCachedFriends() {
        viewModelScope.launch { replaceFriendsFromCache() }
    }

    private suspend fun replaceFriendsFromCache() {
        _friends.replaceWith(repository.loadCachedFriends().map(::toFriend))
    }

    private fun showError(throwable: Throwable) {
        errorMessage = context.localizedString(R.string.friends_load_failed)
    }

    private fun <T> MutableList<T>.replaceWith(items: List<T>) {
        clear()
        addAll(items)
    }

    override fun onCleared() {
        offlinePairingManager.close()
        super.onCleared()
    }
}
