package com.example.comingsoon.sync

import com.example.comingsoon.auth.AuthenticatedUser
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class ServerLiveLocationPoint(
    val latitude: Double,
    val longitude: Double,
    @SerializedName("accuracy_meters") val accuracyMeters: Float,
    @SerializedName("recorded_at") val recordedAt: String
)

data class ServerFriendLiveLocation(
    val user: AuthenticatedUser,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("accuracy_meters") val accuracyMeters: Float,
    @SerializedName("recorded_at") val recordedAt: String,
    @SerializedName("session_started_at") val sessionStartedAt: String,
    val trail: List<ServerLiveLocationPoint>
)

data class LiveLocationSessionStatus(
    val active: Boolean,
    @SerializedName("started_at") val startedAt: String?
)

private data class ServerFriendLiveLocations(val friends: List<ServerFriendLiveLocation>)

class LiveLocationApiException(message: String) : Exception(message)

class LiveLocationApiClient(baseUrl: String, private val gson: Gson = Gson()) {
    private val baseUrl = baseUrl.trim().trimEnd('/')

    suspend fun startSharing(token: String): LiveLocationSessionStatus = gson.fromJson(
        request("POST", "/live-location/sessions", token),
        LiveLocationSessionStatus::class.java
    )

    suspend fun stopSharing(token: String) {
        request("DELETE", "/live-location/sessions", token)
    }

    suspend fun getSharingStatus(token: String): LiveLocationSessionStatus = gson.fromJson(
        request("GET", "/live-location/sessions", token),
        LiveLocationSessionStatus::class.java
    )

    suspend fun pushFix(token: String, latitude: Double, longitude: Double, accuracyMeters: Float, recordedAt: String) {
        val body = JsonObject().apply {
            addProperty("latitude", latitude)
            addProperty("longitude", longitude)
            addProperty("accuracy_meters", accuracyMeters)
            addProperty("recorded_at", recordedAt)
        }
        request("POST", "/live-location/fixes", token, gson.toJson(body))
    }

    suspend fun getFriendsLiveLocations(token: String): List<ServerFriendLiveLocation> = gson.fromJson(
        request("GET", "/live-location/friends", token),
        ServerFriendLiveLocations::class.java
    ).friends

    private suspend fun request(
        method: String,
        path: String,
        token: String,
        body: String? = null
    ): String = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) throw LiveLocationApiException(
            "Die Server-URL wurde noch nicht konfiguriert."
        )

        val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }

        try {
            if (body != null) {
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw LiveLocationApiException(errorMessage(status, response))
            response
        } catch (exception: LiveLocationApiException) {
            throw exception
        } catch (exception: Exception) {
            throw LiveLocationApiException(
                exception.localizedMessage ?: "Der Server ist nicht erreichbar."
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun errorMessage(status: Int, response: String): String {
        val detail = runCatching {
            gson.fromJson(response, JsonObject::class.java)
                ?.get("detail")?.takeUnless { it.isJsonNull }?.asString
        }.getOrNull()
        return when (status) {
            401 -> "Deine Sitzung ist abgelaufen. Bitte melde dich erneut an."
            409 -> detail ?: "Die Standortfreigabe ist nicht aktiv."
            else -> detail ?: "Die Standort-Synchronisation ist fehlgeschlagen (HTTP $status)."
        }
    }
}
