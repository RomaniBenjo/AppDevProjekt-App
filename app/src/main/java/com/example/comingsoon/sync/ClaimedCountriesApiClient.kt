package com.example.comingsoon.sync

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class ServerClaimedCountry(
    @SerializedName("country_id") val countryId: String,
    val name: String,
    @SerializedName("claimed_at") val claimedAt: Long
)

private data class ServerClaimedCountriesList(
    @SerializedName("claimed_countries") val claimedCountries: List<ServerClaimedCountry>
)

class ClaimedCountriesApiException(message: String) : Exception(message)

class ClaimedCountriesApiClient(baseUrl: String, private val gson: Gson = Gson()) {
    private val baseUrl = baseUrl.trim().trimEnd('/')

    suspend fun listClaims(token: String): List<ServerClaimedCountry> = gson.fromJson(
        request("GET", "/claimed-countries", token),
        ServerClaimedCountriesList::class.java
    ).claimedCountries

    suspend fun claimCountry(token: String, countryId: String, name: String, claimedAt: Long): ServerClaimedCountry {
        val body = JsonObject().apply {
            addProperty("country_id", countryId)
            addProperty("name", name)
            addProperty("claimed_at", claimedAt)
        }
        return gson.fromJson(
            request("POST", "/claimed-countries", token, gson.toJson(body)),
            ServerClaimedCountry::class.java
        )
    }

    suspend fun clearAllClaims(token: String) {
        request("DELETE", "/claimed-countries", token)
    }

    private suspend fun request(
        method: String,
        path: String,
        token: String,
        body: String? = null
    ): String = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) throw ClaimedCountriesApiException(
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
            if (status !in 200..299) throw ClaimedCountriesApiException(errorMessage(status, response))
            response
        } catch (exception: ClaimedCountriesApiException) {
            throw exception
        } catch (exception: Exception) {
            throw ClaimedCountriesApiException(
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
            422 -> detail ?: "Das Land konnte nicht gespeichert werden."
            else -> detail ?: "Die Synchronisation der Länder ist fehlgeschlagen (HTTP $status)."
        }
    }
}
