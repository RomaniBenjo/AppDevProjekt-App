package com.example.comingsoon.friends

import com.example.comingsoon.auth.AuthSessionStore
class FriendsRepository(
    private val apiClient: FriendsApiClient,
    private val sessionStore: AuthSessionStore
) {
    fun hasSession(): Boolean = sessionStore.load() != null
    fun currentUserId(): Int? = sessionStore.load()?.user?.id?.toInt()

    suspend fun loadFriends() = apiClient.getFriends(token())
    suspend fun loadRequests() = apiClient.getRequests(token())
    suspend fun searchUsers(query: String) = apiClient.searchUsers(token(), query)
    suspend fun sendRequest(userId: Int) = apiClient.sendRequest(token(), userId)
    suspend fun acceptRequest(requestId: Int) = apiClient.acceptRequest(token(), requestId)
    suspend fun deleteRequest(requestId: Int) = apiClient.deleteRequest(token(), requestId)
    suspend fun removeFriend(friendId: Int) = apiClient.removeFriend(token(), friendId)

    private fun token(): String = sessionStore.load()?.accessToken
        ?: throw FriendsApiException("Deine Sitzung ist abgelaufen. Bitte melde dich erneut an.")
}
