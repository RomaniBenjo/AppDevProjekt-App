package com.example.comingsoon.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comingsoon.auth.AuthenticatedUser
import com.example.comingsoon.friends.FriendsRepository
import com.example.comingsoon.friends.ServerFriendRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant

data class Friend(
    val id: Int,
    val name: String,
    val email: String = "",
    val image: Int? = null,
    val imageUrl: String? = null,
    val sharedWithMe: List<Journey> = emptyList(),
    val sharedByMe: List<Journey> = emptyList(),
    val liveLocation: FriendLocation? = null
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
    private val repository: FriendsRepository
) : ViewModel() {
    private var realtimeJob: Job? = null

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

    val currentUserId: Int?
        get() = repository.currentUserId()

    init {
        if (repository.hasSession()) {
            refresh()
            startRealtimeUpdates()
        }
    }

    fun getFriend(id: Int): Friend? = _friends.find { it.id == id }

    fun refresh() {
        refresh(showLoading = true, showErrors = true)
    }

    fun startRealtimeUpdates() {
        if (!repository.hasSession() || realtimeJob?.isActive == true) return
        realtimeJob = viewModelScope.launch {
            var retryDelayMillis = 1_000L
            while (isActive && repository.hasSession()) {
                try {
                    repository.listenForUpdates {
                        refresh(showLoading = false, showErrors = false)
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
    }

    private fun refresh(showLoading: Boolean, showErrors: Boolean) {
        viewModelScope.launch {
            if (showLoading) isLoading = true
            if (showErrors) errorMessage = null
            runCatching {
                coroutineScope {
                    val friends = async { repository.loadFriends() }
                    val requests = async { repository.loadRequests() }
                    friends.await() to requests.await()
                }
            }.onSuccess { (serverFriends, requests) ->
                _friends.replaceWith(serverFriends.map(::toFriend))
                _incomingRequests.replaceWith(
                    requests.incoming.map { it.toRequest(incoming = true) }
                )
                _outgoingRequests.replaceWith(
                    requests.outgoing.map { it.toRequest(incoming = false) }
                )
            }.onFailure { throwable ->
                if (showErrors) showError(throwable)
            }
            if (showLoading) isLoading = false
        }
    }

    fun searchFriends(query: String) {
        val normalized = query.trim()
        if (normalized.length < 2) {
            _searchResults.clear()
            errorMessage = "Die Suche muss mindestens zwei Zeichen enthalten."
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

    fun removeFriend(id: Int) = mutate {
        repository.removeFriend(id)
        _friends.removeAll { it.id == id }
    }

    fun clearError() {
        errorMessage = null
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
        val friends = repository.loadFriends()
        val requests = repository.loadRequests()
        _friends.replaceWith(friends.map(::toFriend))
        _incomingRequests.replaceWith(requests.incoming.map { it.toRequest(true) })
        _outgoingRequests.replaceWith(requests.outgoing.map { it.toRequest(false) })
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

    private fun showError(throwable: Throwable) {
        errorMessage = throwable.message ?: "Die Friends-Daten konnten nicht geladen werden."
    }

    private fun <T> MutableList<T>.replaceWith(items: List<T>) {
        clear()
        addAll(items)
    }
}
