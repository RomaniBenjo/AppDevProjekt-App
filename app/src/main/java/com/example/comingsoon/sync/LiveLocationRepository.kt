package com.example.comingsoon.sync

import com.example.comingsoon.auth.AuthSessionStore

class LiveLocationRepository(
    private val apiClient: LiveLocationApiClient,
    private val sessionStore: AuthSessionStore
) {
    fun hasSession(): Boolean = sessionStore.load() != null

    suspend fun startSharing(): LiveLocationSessionStatus = apiClient.startSharing(token())

    suspend fun stopSharing() = apiClient.stopSharing(token())

    suspend fun getSharingStatus(): LiveLocationSessionStatus = apiClient.getSharingStatus(token())

    suspend fun pushFix(latitude: Double, longitude: Double, accuracyMeters: Float, recordedAt: String) =
        apiClient.pushFix(token(), latitude, longitude, accuracyMeters, recordedAt)

    suspend fun getFriendsLiveLocations(): List<ServerFriendLiveLocation> =
        apiClient.getFriendsLiveLocations(token())

    fun currentToken(): String? = sessionStore.load()?.accessToken

    private fun token(): String = sessionStore.load()?.accessToken
        ?: throw LiveLocationApiException("Deine Sitzung ist abgelaufen. Bitte melde dich erneut an.")
}
