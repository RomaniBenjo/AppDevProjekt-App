package com.example.comingsoon.friends

import com.example.comingsoon.auth.AuthenticatedUser
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class ServerFriendRequest(
    val id: Int,
    val sender: AuthenticatedUser,
    val receiver: AuthenticatedUser,
    @SerializedName("created_at") val createdAt: String
)

data class ServerFriendRequests(
    val incoming: List<ServerFriendRequest>,
    val outgoing: List<ServerFriendRequest>
)

private data class ServerFriends(val friends: List<AuthenticatedUser>)

class FriendsApiException(message: String) : Exception(message)

class FriendsApiClient(
    baseUrl: String,
    private val gson: Gson = Gson()
) {
    private val baseUrl = baseUrl.trim().trimEnd('/')

    suspend fun searchUsers(token: String, query: String): List<AuthenticatedUser> {
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        val json = request("GET", "/users/search?q=$encoded", token)
        val type = object : TypeToken<List<AuthenticatedUser>>() {}.type
        return gson.fromJson(json, type)
    }

    suspend fun sendRequest(token: String, userId: Int): ServerFriendRequest {
        val body = JsonObject().apply { addProperty("user_id", userId) }
        return gson.fromJson(
            request("POST", "/friends/requests", token, gson.toJson(body)),
            ServerFriendRequest::class.java
        )
    }

    suspend fun getRequests(token: String): ServerFriendRequests = gson.fromJson(
        request("GET", "/friends/requests", token),
        ServerFriendRequests::class.java
    )

    suspend fun acceptRequest(token: String, requestId: Int) {
        request("POST", "/friends/requests/$requestId/accept", token)
    }

    suspend fun deleteRequest(token: String, requestId: Int) {
        request("DELETE", "/friends/requests/$requestId", token)
    }

    suspend fun getFriends(token: String): List<AuthenticatedUser> = gson.fromJson(
        request("GET", "/friends", token),
        ServerFriends::class.java
    ).friends

    suspend fun removeFriend(token: String, friendId: Int) {
        request("DELETE", "/friends/$friendId", token)
    }

    private suspend fun request(
        method: String,
        path: String,
        token: String,
        body: String? = null
    ): String = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) throw FriendsApiException(
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
            if (status !in 200..299) throw FriendsApiException(errorMessage(status, response))
            response
        } catch (exception: FriendsApiException) {
            throw exception
        } catch (exception: Exception) {
            throw FriendsApiException(
                exception.localizedMessage ?: "Der Friends-Server ist nicht erreichbar."
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
            404 -> detail ?: "Benutzer oder Freundschaft wurde nicht gefunden."
            409 -> detail ?: "Diese Freundschaftsanfrage existiert bereits."
            422 -> detail ?: "Die Suche muss mindestens zwei Zeichen enthalten."
            else -> detail ?: "Die Friends-Anfrage ist fehlgeschlagen (HTTP $status)."
        }
    }
}
