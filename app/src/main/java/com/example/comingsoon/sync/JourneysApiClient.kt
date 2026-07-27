package com.example.comingsoon.sync

import com.example.comingsoon.auth.AuthenticatedUser
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class ServerJourneyLocation(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

data class ServerJourney(
    val id: Int,
    val title: String,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    val shared: Boolean?,
    val locations: List<ServerJourneyLocation>,
    @SerializedName("visited_countries") val visitedCountries: List<String>
)

data class JourneyUpsertBody(
    val title: String,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    val shared: Boolean?,
    val locations: List<ServerJourneyLocation>,
    @SerializedName("visited_countries") val visitedCountries: List<String>
)

private data class ServerJourneysList(val journeys: List<ServerJourney>)

data class ServerJourneyShare(
    @SerializedName("owner_id") val ownerId: Int,
    @SerializedName("recipient_id") val recipientId: Int,
    val owner: AuthenticatedUser,
    @SerializedName("share_type") val shareType: String,
    @SerializedName("created_at") val createdAt: String,
    val journey: ServerJourney
)

private data class ServerJourneySharesList(val shares: List<ServerJourneyShare>)
data class ServerJourneyShareLink(
    @SerializedName("deep_link") val deepLink: String,
    @SerializedName("expires_at") val expiresAt: String
)

class JourneysApiException(
    message: String,
    val statusCode: Int? = null
) : Exception(message)

class JourneysApiClient(baseUrl: String, private val gson: Gson = Gson()) {
    private val baseUrl = baseUrl.trim().trimEnd('/')

    suspend fun listJourneys(token: String): List<ServerJourney> = gson.fromJson(
        request("GET", "/journeys", token),
        ServerJourneysList::class.java
    ).journeys

    suspend fun createJourney(token: String, body: JourneyUpsertBody): ServerJourney = gson.fromJson(
        request("POST", "/journeys", token, gson.toJson(body)),
        ServerJourney::class.java
    )

    suspend fun updateJourney(token: String, serverId: Int, body: JourneyUpsertBody): ServerJourney = gson.fromJson(
        request("PUT", "/journeys/$serverId", token, gson.toJson(body)),
        ServerJourney::class.java
    )

    suspend fun deleteJourney(token: String, serverId: Int) {
        request("DELETE", "/journeys/$serverId", token)
    }

    suspend fun listJourneyShares(token: String): List<ServerJourneyShare> = gson.fromJson(
        request("GET", "/journey-shares", token),
        ServerJourneySharesList::class.java
    ).shares

    suspend fun shareJourney(token: String, serverId: Int, friendUserId: Int) {
        val body = JsonObject().apply { addProperty("friend_user_id", friendUserId) }
        request("POST", "/journeys/$serverId/shares", token, gson.toJson(body))
    }

    suspend fun unshareJourney(token: String, serverId: Int, friendUserId: Int) {
        request("DELETE", "/journeys/$serverId/shares/$friendUserId", token)
    }

    suspend fun createShareLink(token: String, serverId: Int): ServerJourneyShareLink =
        gson.fromJson(
            request("POST", "/journeys/$serverId/share-link", token),
            ServerJourneyShareLink::class.java
        )

    suspend fun acceptShareLink(token: String, shareToken: String): ServerJourneyShare =
        gson.fromJson(
            request("POST", "/journey-share-links/$shareToken/accept", token),
            ServerJourneyShare::class.java
        )

    private suspend fun request(
        method: String,
        path: String,
        token: String,
        body: String? = null
    ): String = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) throw JourneysApiException(
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
            if (status !in 200..299) {
                throw JourneysApiException(errorMessage(status, response), status)
            }
            response
        } catch (exception: JourneysApiException) {
            throw exception
        } catch (exception: Exception) {
            throw JourneysApiException(
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
            403 -> detail ?: "Reisen können nur mit Freunden geteilt werden."
            404 -> detail ?: "Reise wurde nicht gefunden."
            409 -> detail ?: "Dieser Freigabelink wurde bereits verwendet."
            410 -> detail ?: "Dieser Freigabelink ist abgelaufen."
            422 -> detail ?: "Die Reisedaten sind ungültig."
            else -> detail ?: "Die Reise-Synchronisation ist fehlgeschlagen (HTTP $status)."
        }
    }
}
