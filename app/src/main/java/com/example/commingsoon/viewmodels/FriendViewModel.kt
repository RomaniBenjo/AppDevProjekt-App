package com.example.commingsoon.viewmodels

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import java.time.Instant

data class Friend(
    val id: Int,
    val name: String,
    val image: Int? = null,
    val sharedWithMe: List<Journey>? = null,
    val sharedByMe: List<Journey>? = null,
    val liveLocation: FriendLocation?
)

data class FriendLocation(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Instant? = null
)

class FriendViewModel : ViewModel() {
    private val _friends = mutableStateListOf<Friend>()
    val friends: List<Friend>
        get() = _friends
    init {
        _friends.addAll(FriendPlaceholder.friends)
    }

    fun getFriend(id: Int): Friend? {
        return _friends.find { it.id == id }
    }
    fun addFriend(friend: Friend) {
        _friends.add(friend)
    }
    fun removeFriend(id: Int) {
        _friends.removeIf { it.id == id }
    }
    fun updateFriend(updatedFriend: Friend) {
        val index = _friends.indexOfFirst {
            it.id == updatedFriend.id
        }

        if (index != -1) {
            _friends[index] = updatedFriend
        }
    }

    fun getNextFriendId(): Int {
        return (_friends.maxOfOrNull { it.id } ?: 0) + 1
    }
}