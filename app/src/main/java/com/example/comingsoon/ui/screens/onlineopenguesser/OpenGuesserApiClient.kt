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
    val longitude: Double
)

data class OpenGuesserGame(
    val id: String,
    val status: String,
    @SerializedName("round_number") val roundNumber: Int,
    @SerializedName("total_rounds") val totalRounds: Int,
    val host: AuthenticatedUser,
    val participants: List<AuthenticatedUser>,
    val panorama: OpenGuesserPanorama?
)

class OpenGuesserApiException(message: String) : Exception(message)

sealed interface OpenGuesserSocketEvent {
    data class LobbiesUpdated(val lobbies: List<OpenGuesserGame>) : OpenGuesserSocketEvent
    data class GameUpdated(val game: OpenGuesserGame) : OpenGuesserSocketEvent
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

    suspend fun startLobby(gameId: String, accessToken: String): OpenGuesserGame =
        gameRequest("start_lobby", gameId, accessToken)

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

    private suspend fun gameRequest(action: String, gameId: String?, accessToken: String): OpenGuesserGame =
        gson.fromJson(request(action, gameId, accessToken).getAsJsonObject("game"), OpenGuesserGame::class.java)

    private suspend fun request(action: String, gameId: String?, accessToken: String): JsonObject {
        ensureConnected(accessToken)
        val requestId = UUID.randomUUID().toString()
        val response = CompletableDeferred<JsonObject>()
        pendingRequests[requestId] = response
        val message = JsonObject().apply {
            addProperty("request_id", requestId)
            addProperty("action", action)
            gameId?.let { addProperty("game_id", it) }
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
