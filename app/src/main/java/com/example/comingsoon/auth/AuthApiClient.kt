package com.example.comingsoon.auth

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class AuthenticatedUser(
    val id: Long,
    val email: String,
    val name: String?,
    @SerializedName("picture_url") val pictureUrl: String?
)

data class ServerAuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Long,
    val user: AuthenticatedUser
)

class AuthApiException(message: String) : Exception(message)

class AuthApiClient(
    baseUrl: String,
    private val gson: Gson = Gson()
) {
    private val baseUrl = baseUrl.trim().trimEnd('/')

    suspend fun authenticateWithGoogle(idToken: String): ServerAuthResponse =
        withContext(Dispatchers.IO) {
            require(baseUrl.isNotBlank()) { "Die Server-URL wurde noch nicht konfiguriert." }

            val connection = (URL("$baseUrl/auth/google").openConnection() as HttpURLConnection)
                .apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                }

            try {
                val body = JsonObject().apply { addProperty("id_token", idToken) }
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use {
                    it.write(gson.toJson(body))
                }

                val responseText = connection.responseText()
                if (connection.responseCode !in 200..299) {
                    throw AuthApiException(errorDetail(responseText, connection.responseCode))
                }

                gson.fromJson(responseText, ServerAuthResponse::class.java)
                    ?: throw AuthApiException("Der Server hat keine Anmeldedaten geliefert.")
            } catch (exception: AuthApiException) {
                throw exception
            } catch (exception: Exception) {
                throw AuthApiException(
                    exception.localizedMessage ?: "Der Authentifizierungsserver ist nicht erreichbar."
                )
            } finally {
                connection.disconnect()
            }
        }

    private fun HttpURLConnection.responseText(): String {
        val stream = if (responseCode in 200..299) inputStream else errorStream
        return stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
    }

    private fun errorDetail(responseText: String, statusCode: Int): String {
        val detail = runCatching {
            gson.fromJson(responseText, JsonObject::class.java)
                ?.get("detail")
                ?.takeUnless { it.isJsonNull }
                ?.asString
        }.getOrNull()

        return when (statusCode) {
            401 -> "Google konnte vom Server nicht bestätigt werden. Bitte melde dich erneut an."
            409 -> detail ?: "Diese E-Mail-Adresse ist bereits mit einem anderen Konto verknüpft."
            else -> detail ?: "Die Anmeldung am Server ist fehlgeschlagen (HTTP $statusCode)."
        }
    }
}
