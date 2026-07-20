package com.example.commingsoon.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.commingsoon.auth.AuthenticatedUser
import com.example.commingsoon.friends.FriendsRepository
import com.example.commingsoon.friends.ServerFriendRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

    init {
        if (repository.hasSession()) refresh()
    }

    fun getFriend(id: Int): Friend? = _friends.find { it.id == id }

    fun refresh() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
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
            }.onFailure(::showError)
            isLoading = false
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

    fun sendFriendRequest(friend: Friend) = mutate {
        repository.sendRequest(friend.id)
        _searchResults.removeAll { it.id == friend.id }
        refreshAfterMutation()
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
