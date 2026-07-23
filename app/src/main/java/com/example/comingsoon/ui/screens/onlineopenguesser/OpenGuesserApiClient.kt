package com.example.comingsoon.ui.screens.onlineopenguesser

import com.example.comingsoon.auth.AuthenticatedUser
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.UUID

data class OpenGuesserPanorama(
    val latitude: Double,
    val longitude: Double,
    @SerializedName("pano_id") val panoId: String? = null
)

data class OpenGuesserScore(
    val user: AuthenticatedUser,
    @SerializedName("total_points") val totalPoints: Int,
    @SerializedName("round_points") val roundPoints: Int?,
    @SerializedName("distance_km") val distanceKm: Double?,
    val guess: OpenGuesserPanorama?
)

data class OpenGuesserRoundResult(
    @SerializedName("round_number") val roundNumber: Int,
    @SerializedName("actual_location") val actualLocation: OpenGuesserPanorama,
    val scores: List<OpenGuesserScore>
)

data class OpenGuesserGame(
    val id: String,
    val status: String,
    @SerializedName("round_number") val roundNumber: Int,
    @SerializedName("total_rounds") val totalRounds: Int,
    @SerializedName("round_seconds") val roundSeconds: Int,
    @SerializedName("round_started_at") val roundStartedAt: String?,
    @SerializedName("location_filter") val locationFilter: String,
    val host: AuthenticatedUser,
    val participants: List<AuthenticatedUser>,
    val panorama: OpenGuesserPanorama?,
    @SerializedName("round_complete") val roundComplete: Boolean,
    @SerializedName("current_user_submitted") val currentUserSubmitted: Boolean,
    val scores: List<OpenGuesserScore>,
    @SerializedName("actual_location") val actualLocation: OpenGuesserPanorama?,
    @SerializedName("round_results") val roundResults: List<OpenGuesserRoundResult>
)

class OpenGuesserApiException(message: String) : Exception(message)

sealed interface OpenGuesserSocketEvent {
    data class LobbiesUpdated(val lobbies: List<OpenGuesserGame>) : OpenGuesserSocketEvent
    data class GameUpdated(val game: OpenGuesserGame) : OpenGuesserSocketEvent
    data class GameClosed(val gameId: String) : OpenGuesserSocketEvent
}

class OpenGuesserApiClient(
    baseUrl: String,
    private val gson: Gson = Gson(),
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    private val baseUrl = baseUrl.trim().trimEnd('/')
    private var socket: WebSocket? = null
    private var connectedToken: String? = null
    private var connectWaiter: CompletableDeferred<Unit>? = null
    private val pendingRequests = mutableMapOf<String, CompletableDeferred<JsonObject>>()
    private val _events = MutableSharedFlow<OpenGuesserSocketEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    suspend fun startGame(accessToken: String): OpenGuesserGame =
        gameRequest("start_game", null, accessToken)

    suspend fun createLobby(accessToken: String): OpenGuesserGame =
        gameRequest("create_lobby", null, accessToken)

    suspend fun listLobbies(accessToken: String): List<OpenGuesserGame> =
        request("list_lobbies", null, accessToken).getAsJsonArray("lobbies").map {
            gson.fromJson(it, OpenGuesserGame::class.java)
        }

    suspend fun joinLobby(gameId: String, accessToken: String): OpenGuesserGame =
        gameRequest("join_lobby", gameId, accessToken)

    suspend fun startLobby(
        gameId: String,
        totalRounds: Int,
        roundSeconds: Int,
        locationFilter: String,
        accessToken: String
    ): OpenGuesserGame = gameRequest(
        "start_lobby", gameId, accessToken,
        mapOf("total_rounds" to totalRounds, "round_seconds" to roundSeconds),
        mapOf("location_filter" to locationFilter)
    )

    suspend fun submitGuess(gameId: String, latitude: Double, longitude: Double, accessToken: String): OpenGuesserGame =
        gameRequest("submit_guess", gameId, accessToken, mapOf("latitude" to latitude, "longitude" to longitude))

    suspend fun nextRound(gameId: String, accessToken: String): OpenGuesserGame =
        gameRequest("next_round", gameId, accessToken)

    suspend fun replaceUnavailableLocation(
        gameId: String,
        latitude: Double,
        longitude: Double,
        accessToken: String
    ): OpenGuesserGame = gameRequest(
        "replace_unavailable_location", gameId, accessToken,
        mapOf("latitude" to latitude, "longitude" to longitude)
    )

    suspend fun leaveGame(gameId: String, accessToken: String) {
        request("leave_game", gameId, accessToken)
    }

    suspend fun leaveLobby(gameId: String, accessToken: String) {
        request("leave_lobby", gameId, accessToken)
    }

    suspend fun getGame(gameId: String, accessToken: String): OpenGuesserGame =
        gameRequest("get_game", gameId, accessToken)

    fun close() {
        socket?.close(1000, "OpenGuesser screen closed")
        socket = null
        connectedToken = null
    }

    private suspend fun gameRequest(
        action: String,
        gameId: String?,
        accessToken: String,
        properties: Map<String, Number> = emptyMap(),
        textProperties: Map<String, String> = emptyMap()
    ): OpenGuesserGame = gson.fromJson(
        request(action, gameId, accessToken, properties, textProperties).getAsJsonObject("game"),
        OpenGuesserGame::class.java
    )

    private suspend fun request(
        action: String,
        gameId: String?,
        accessToken: String,
        properties: Map<String, Number> = emptyMap(),
        textProperties: Map<String, String> = emptyMap()
    ): JsonObject {
        ensureConnected(accessToken)
        val requestId = UUID.randomUUID().toString()
        val response = CompletableDeferred<JsonObject>()
        pendingRequests[requestId] = response
        val message = JsonObject().apply {
            addProperty("request_id", requestId)
            addProperty("action", action)
            gameId?.let { addProperty("game_id", it) }
            properties.forEach { (key, value) -> addProperty(key, value) }
            textProperties.forEach { (key, value) -> addProperty(key, value) }
        }
        if (socket?.send(gson.toJson(message)) != true) {
            pendingRequests.remove(requestId)
            throw OpenGuesserApiException("The OpenGuesser connection is closed.")
        }
        return response.await().also { pendingRequests.remove(requestId) }
    }

    private suspend fun ensureConnected(accessToken: String) {
        require(baseUrl.isNotBlank()) { "The server URL has not been configured." }
        require(accessToken.isNotBlank()) { "Sign in again before starting OpenGuesser." }
        if (socket != null && connectedToken == accessToken) return
        close()
        val waiter = CompletableDeferred<Unit>()
        connectWaiter = waiter
        val websocketUrl = baseUrl.replaceFirst(Regex("^https"), "wss")
            .replaceFirst(Regex("^http"), "ws") + "/ws/openguesser"
        socket = httpClient.newWebSocket(
            Request.Builder().url(websocketUrl).header("Authorization", "Bearer $accessToken").build(),
            listener
        )
        connectedToken = accessToken
        waiter.await()
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            connectWaiter?.complete(Unit)
        }
        override fun onMessage(webSocket: WebSocket, text: String) {
            runCatching { gson.fromJson(text, JsonObject::class.java) }.getOrNull()?.let { message ->
                when (message.get("type")?.asString) {
                    "response" -> {
                        val request = pendingRequests[message.get("request_id")?.asString] ?: return@let
                        if (message.get("ok")?.asBoolean == true) request.complete(message)
                        else request.completeExceptionally(OpenGuesserApiException(message.get("error")?.asString ?: "OpenGuesser request failed."))
                    }
                    "lobbies_updated" -> _events.tryEmit(OpenGuesserSocketEvent.LobbiesUpdated(message.getAsJsonArray("lobbies").map { gson.fromJson(it, OpenGuesserGame::class.java) }))
                    "game_updated" -> _events.tryEmit(OpenGuesserSocketEvent.GameUpdated(gson.fromJson(message.getAsJsonObject("game"), OpenGuesserGame::class.java)))
                    "game_closed" -> _events.tryEmit(
                        OpenGuesserSocketEvent.GameClosed(message.get("game_id")?.asString.orEmpty())
                    )
                }
            }
        }
        override fun onFailure(webSocket: WebSocket, throwable: Throwable, response: Response?) = fail(throwable)
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = fail(OpenGuesserApiException(reason.ifBlank { "OpenGuesser connection closed." }))
        private fun fail(error: Throwable) {
            connectWaiter?.completeExceptionally(error)
            pendingRequests.values.forEach { it.completeExceptionally(error) }
            pendingRequests.clear()
            socket = null
            connectedToken = null
        }
    }
}
