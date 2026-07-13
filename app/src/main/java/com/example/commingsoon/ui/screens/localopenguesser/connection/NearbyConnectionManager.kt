package com.example.commingsoon.ui.screens.localopenguesser.connection

import android.content.Context
import com.example.commingsoon.ui.screens.localopenguesser.IndexedPhoto
import com.example.commingsoon.ui.screens.localopenguesser.PhotoIndexDatabase
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

internal class NearbyConnectionManager(context: Context) {
    private val appContext = context.applicationContext
    private val client: ConnectionsClient = Nearby.getConnectionsClient(appContext)
    private val serviceId = "${appContext.packageName}.localopenguesser.v1"
    private val strategy = Strategy.P2P_POINT_TO_POINT
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val endpointNames = mutableMapOf<String, String>()
    private val mutableState = MutableStateFlow(NearbyConnectionState())
    val state: StateFlow<NearbyConnectionState> = mutableState.asStateFlow()

    private var selectedPhotos: List<IndexedPhoto> = emptyList()
    private var timerJob: Job? = null
    private var receivedOtherPhoto = false
    private var otherPlayerReceivedOurPhoto = false
    private val incomingPayloads = mutableMapOf<Long, Payload>()
    private val incomingMetadata = mutableMapOf<Long, Int>()
    private val completedIncomingPayloads = mutableSetOf<Long>()
    private val processingPayloads = mutableSetOf<Long>()
    private val outgoingPayloads = mutableMapOf<Long, Pair<Payload, File>>()

    fun setLocalName(name: String) {
        mutableState.value = mutableState.value.copy(localName = name.trim().take(32))
    }

    fun hasGooglePlayServices(): Boolean = GoogleApiAvailability.getInstance()
        .isGooglePlayServicesAvailable(appContext) == ConnectionResult.SUCCESS

    fun startHosting() {
        val localName = mutableState.value.localName.ifBlank { "Local OpenGuesser Host" }
        stopSearchOperations()
        mutableState.value = mutableState.value.copy(
            phase = NearbyPhase.ADVERTISING,
            role = NearbyRole.HOST,
            discoveredEndpoints = emptyList(),
            errorMessage = null
        )
        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        client.startAdvertising(localName, serviceId, connectionLifecycleCallback, options)
            .addOnFailureListener { showConnectionError("Could not host a nearby game", it) }
    }

    fun startJoining() {
        stopSearchOperations()
        mutableState.value = mutableState.value.copy(
            phase = NearbyPhase.DISCOVERING,
            role = NearbyRole.JOINER,
            discoveredEndpoints = emptyList(),
            errorMessage = null
        )
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        client.startDiscovery(serviceId, endpointDiscoveryCallback, options)
            .addOnFailureListener { showConnectionError("Could not search for nearby games", it) }
    }

    fun requestConnection(endpoint: NearbyEndpoint) {
        val localName = mutableState.value.localName.ifBlank { "Local OpenGuesser Player" }
        client.stopDiscovery()
        mutableState.value = mutableState.value.copy(
            phase = NearbyPhase.REQUESTING_CONNECTION,
            errorMessage = null
        )
        client.requestConnection(localName, endpoint.id, connectionLifecycleCallback)
            .addOnFailureListener { showConnectionError("Could not request the connection", it) }
    }

    fun acceptPendingConnection() {
        val pending = mutableState.value.pendingConnection ?: return
        mutableState.value = mutableState.value.copy(
            phase = NearbyPhase.CONNECTING,
            pendingConnection = null
        )
        client.acceptConnection(pending.endpoint.id, payloadCallback)
            .addOnFailureListener { showConnectionError("Could not accept the connection", it) }
    }

    fun rejectPendingConnection() {
        val pending = mutableState.value.pendingConnection ?: return
        client.rejectConnection(pending.endpoint.id)
        mutableState.value = mutableState.value.copy(
            phase = NearbyPhase.IDLE,
            pendingConnection = null
        )
    }

    fun startGame(settings: LocalGameSettings) {
        if (mutableState.value.role != NearbyRole.HOST) return
        val safeSettings = settings.copy(
            roundCount = settings.roundCount.coerceIn(1, 20),
            roundSeconds = settings.roundSeconds.coerceIn(10, 300)
        )
        prepareLocalSelection(safeSettings, notifyHostWhenReady = false) {
            sendControl(
                LocalGameProtocol.TYPE_GAME_SETTINGS,
                mapOf(
                    "roundCount" to safeSettings.roundCount,
                    "roundSeconds" to safeSettings.roundSeconds
                )
            )
            updateGame {
                it.copy(
                    phase = LocalGamePhase.WAITING_FOR_OTHER_PLAYER,
                    statusMessage = "Waiting for the other phone to prepare its photos…"
                )
            }
        }
    }

    fun sendTestMessage(message: String) {
        val endpoint = mutableState.value.connectedEndpoint ?: return
        if (message.isBlank()) return
        client.sendPayload(
            endpoint.id,
            Payload.fromBytes(LocalGameProtocol.encodeTestMessage(message.trim()))
        ).addOnFailureListener { showConnectionError("Could not send the test message", it) }
    }

    fun stopSearching() {
        stopSearchOperations()
        client.stopAllEndpoints()
        mutableState.value = mutableState.value.copy(
            phase = NearbyPhase.IDLE,
            role = null,
            discoveredEndpoints = emptyList(),
            pendingConnection = null,
            errorMessage = null
        )
    }

    fun disconnect() {
        timerJob?.cancel()
        mutableState.value.connectedEndpoint?.let { client.disconnectFromEndpoint(it.id) }
        client.stopAllEndpoints()
        stopSearchOperations()
        clearTransferFiles()
        selectedPhotos = emptyList()
        mutableState.value = NearbyConnectionState(localName = mutableState.value.localName)
    }

    fun close() {
        disconnect()
        scope.cancel()
    }

    private fun prepareLocalSelection(
        settings: LocalGameSettings,
        notifyHostWhenReady: Boolean,
        onReady: () -> Unit = {}
    ) {
        updateGame {
            it.copy(
                phase = LocalGamePhase.PREPARING,
                settings = settings,
                statusMessage = "Selecting random photos from this device…"
            )
        }
        scope.launch {
            val selection = withContext(Dispatchers.IO) { selectRandomPhotos(settings.roundCount) }
            if (selection.size != settings.roundCount) {
                val message = "This device does not have enough geotagged photos to create " +
                    "${settings.roundCount} rounds while using at most two photos per country."
                showGameError(message, notifyOtherPlayer = true)
                return@launch
            }
            selectedPhotos = selection
            onReady()
            if (notifyHostWhenReady) {
                updateGame {
                    it.copy(
                        phase = LocalGamePhase.WAITING_FOR_OTHER_PLAYER,
                        statusMessage = "Photos selected. Waiting for the host…"
                    )
                }
                sendControl(LocalGameProtocol.TYPE_PLAYER_READY)
            }
        }
    }

    private fun selectRandomPhotos(roundCount: Int): List<IndexedPhoto> {
        val database = PhotoIndexDatabase(appContext)
        val candidates = try {
            database.readAll().values.filter {
                !it.unreadable && it.latitude != null && it.longitude != null && it.country != null
            }.shuffled()
        } finally {
            database.close()
        }
        val countryCounts = mutableMapOf<String, Int>()
        return candidates.filter { photo ->
            val country = photo.country ?: return@filter false
            val count = countryCounts.getOrDefault(country, 0)
            if (count >= MAX_PHOTOS_PER_COUNTRY) {
                false
            } else {
                countryCounts[country] = count + 1
                true
            }
        }.take(roundCount)
    }

    private fun startRound(round: Int) {
        val settings = mutableState.value.game.settings
        if (round >= settings.roundCount) {
            sendControl(LocalGameProtocol.TYPE_GAME_FINISHED)
            finishGame()
            return
        }
        sendControl(LocalGameProtocol.TYPE_ROUND_START, mapOf("round" to round))
        beginRound(round)
    }

    private fun beginRound(round: Int) {
        if (round !in selectedPhotos.indices) {
            showGameError("Round ${round + 1} has no selected local photo.", notifyOtherPlayer = true)
            return
        }
        timerJob?.cancel()
        receivedOtherPhoto = false
        otherPlayerReceivedOurPhoto = false
        mutableState.value.game.receivedPhotoPath?.let { File(it).delete() }
        updateGame {
            it.copy(
                phase = LocalGamePhase.TRANSFERRING_PHOTO,
                currentRound = round,
                receivedPhotoPath = null,
                transferProgress = 0f,
                secondsRemaining = it.settings.roundSeconds,
                statusMessage = "Exchanging round ${round + 1} photos…"
            )
        }
        sendRoundPhoto(round)
    }

    private fun sendRoundPhoto(round: Int) {
        val endpoint = mutableState.value.connectedEndpoint ?: return
        val photo = selectedPhotos.getOrNull(round) ?: return
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    preparePhotoForTransfer(appContext, photo, round)
                }
            }.onSuccess { file ->
                val payload = Payload.fromFile(file)
                outgoingPayloads[payload.id] = payload to file
                sendControl(
                    LocalGameProtocol.TYPE_PHOTO_METADATA,
                    mapOf("payloadId" to payload.id, "round" to round)
                )
                client.sendPayload(endpoint.id, payload).addOnFailureListener {
                    outgoingPayloads.remove(payload.id)
                    payload.close()
                    file.delete()
                    showGameError("Could not send this round's photo.", notifyOtherPlayer = true)
                }
            }.onFailure {
                showGameError("Could not prepare this round's photo.", notifyOtherPlayer = true)
            }
        }
    }

    private fun revealRound() {
        val seconds = mutableState.value.game.settings.roundSeconds
        updateGame {
            it.copy(
                phase = LocalGamePhase.PLAYING_ROUND,
                secondsRemaining = seconds,
                transferProgress = 1f,
                statusMessage = null
            )
        }
        timerJob?.cancel()
        timerJob = scope.launch {
            for (remaining in seconds downTo 1) {
                updateGame { it.copy(secondsRemaining = remaining) }
                delay(1_000)
            }
            updateGame { it.copy(secondsRemaining = 0) }
            if (mutableState.value.role == NearbyRole.HOST) {
                startRound(mutableState.value.game.currentRound + 1)
            }
        }
    }

    private fun maybeRevealRound() {
        if (mutableState.value.role != NearbyRole.HOST) return
        if (!receivedOtherPhoto || !otherPlayerReceivedOurPhoto) return
        sendControl(
            LocalGameProtocol.TYPE_ROUND_REVEAL,
            mapOf("round" to mutableState.value.game.currentRound)
        )
        revealRound()
    }

    private fun finishGame() {
        timerJob?.cancel()
        updateGame {
            it.copy(
                phase = LocalGamePhase.FINISHED,
                secondsRemaining = 0,
                statusMessage = "All ${it.settings.roundCount} rounds are complete."
            )
        }
    }

    private fun handleControlMessage(json: JSONObject) {
        when (json.optString("type")) {
            "test_message" -> {
                json.optString("message").takeIf(String::isNotBlank)?.let { message ->
                    mutableState.value = mutableState.value.copy(receivedTestMessage = message)
                }
            }
            LocalGameProtocol.TYPE_GAME_SETTINGS -> {
                if (mutableState.value.role != NearbyRole.JOINER) return
                val settings = LocalGameSettings(
                    roundCount = json.optInt("roundCount").coerceIn(1, 20),
                    roundSeconds = json.optInt("roundSeconds").coerceIn(10, 300)
                )
                prepareLocalSelection(settings, notifyHostWhenReady = true)
            }
            LocalGameProtocol.TYPE_PLAYER_READY -> {
                if (mutableState.value.role == NearbyRole.HOST &&
                    mutableState.value.game.phase == LocalGamePhase.WAITING_FOR_OTHER_PLAYER &&
                    mutableState.value.game.currentRound == -1
                ) {
                    startRound(0)
                }
            }
            LocalGameProtocol.TYPE_ROUND_START -> beginRound(json.optInt("round", -1))
            LocalGameProtocol.TYPE_PHOTO_METADATA -> {
                val payloadId = json.optLong("payloadId", Long.MIN_VALUE)
                val round = json.optInt("round", -1)
                if (payloadId != Long.MIN_VALUE && round >= 0) {
                    incomingMetadata[payloadId] = round
                    processIncomingPhoto(payloadId)
                }
            }
            LocalGameProtocol.TYPE_PHOTO_READY -> {
                if (mutableState.value.role == NearbyRole.HOST &&
                    json.optInt("round", -1) == mutableState.value.game.currentRound
                ) {
                    otherPlayerReceivedOurPhoto = true
                    maybeRevealRound()
                }
            }
            LocalGameProtocol.TYPE_ROUND_REVEAL -> {
                if (json.optInt("round", -1) == mutableState.value.game.currentRound) revealRound()
            }
            LocalGameProtocol.TYPE_GAME_FINISHED -> finishGame()
            LocalGameProtocol.TYPE_GAME_ERROR -> showGameError(
                json.optString("message", "The other phone could not prepare the game."),
                notifyOtherPlayer = false
            )
        }
    }

    private fun processIncomingPhoto(payloadId: Long) {
        val payload = incomingPayloads[payloadId] ?: return
        val round = incomingMetadata[payloadId] ?: return
        if (payloadId !in completedIncomingPayloads || !processingPayloads.add(payloadId)) return
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val uri = checkNotNull(payload.asFile()?.asUri())
                    val directory = File(appContext.cacheDir, "local_openguesser/incoming")
                        .apply { mkdirs() }
                    val destination = File(directory, "round_${round}_${System.nanoTime()}.jpg")
                    appContext.contentResolver.openInputStream(uri).use { input ->
                        destination.outputStream().buffered().use { output ->
                            checkNotNull(input).copyTo(output)
                        }
                    }
                    appContext.contentResolver.delete(uri, null, null)
                    destination
                }
            }.onSuccess { destination ->
                incomingPayloads.remove(payloadId)?.close()
                incomingMetadata.remove(payloadId)
                completedIncomingPayloads.remove(payloadId)
                processingPayloads.remove(payloadId)
                if (round != mutableState.value.game.currentRound) {
                    destination.delete()
                    return@onSuccess
                }
                receivedOtherPhoto = true
                updateGame {
                    it.copy(
                        receivedPhotoPath = destination.absolutePath,
                        transferProgress = 1f,
                        statusMessage = "Photo received. Waiting for both phones…"
                    )
                }
                sendControl(LocalGameProtocol.TYPE_PHOTO_READY, mapOf("round" to round))
                maybeRevealRound()
            }.onFailure {
                processingPayloads.remove(payloadId)
                showGameError("Could not save the received photo.", notifyOtherPlayer = true)
            }
        }
    }

    private fun sendControl(type: String, values: Map<String, Any?> = emptyMap()) {
        val endpoint = mutableState.value.connectedEndpoint ?: return
        client.sendPayload(endpoint.id, Payload.fromBytes(LocalGameProtocol.encode(type, values)))
            .addOnFailureListener {
                showGameError("Could not synchronize the game with the other phone.", false)
            }
    }

    private fun showGameError(message: String, notifyOtherPlayer: Boolean) {
        timerJob?.cancel()
        if (notifyOtherPlayer) {
            sendControl(LocalGameProtocol.TYPE_GAME_ERROR, mapOf("message" to message))
        }
        updateGame {
            it.copy(
                phase = LocalGamePhase.SETUP,
                receivedPhotoPath = null,
                statusMessage = message
            )
        }
    }

    private fun updateGame(transform: (LocalGameState) -> LocalGameState) {
        mutableState.value = mutableState.value.copy(game = transform(mutableState.value.game))
    }

    private fun stopSearchOperations() {
        client.stopAdvertising()
        client.stopDiscovery()
    }

    private fun showConnectionError(prefix: String, error: Exception) {
        stopSearchOperations()
        mutableState.value = mutableState.value.copy(
            phase = NearbyPhase.ERROR,
            pendingConnection = null,
            errorMessage = "$prefix: ${error.localizedMessage ?: error.javaClass.simpleName}"
        )
    }

    private fun clearTransferFiles() {
        incomingPayloads.values.forEach(Payload::close)
        incomingPayloads.clear()
        incomingMetadata.clear()
        completedIncomingPayloads.clear()
        processingPayloads.clear()
        outgoingPayloads.values.forEach { (payload, file) ->
            payload.close()
            file.delete()
        }
        outgoingPayloads.clear()
        mutableState.value.game.receivedPhotoPath?.let { File(it).delete() }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            endpointNames[endpointId] = info.endpointName
            val endpoint = NearbyEndpoint(endpointId, info.endpointName)
            val endpoints = mutableState.value.discoveredEndpoints
                .filterNot { it.id == endpointId } + endpoint
            mutableState.value = mutableState.value.copy(discoveredEndpoints = endpoints)
        }

        override fun onEndpointLost(endpointId: String) {
            endpointNames.remove(endpointId)
            mutableState.value = mutableState.value.copy(
                discoveredEndpoints = mutableState.value.discoveredEndpoints.filterNot {
                    it.id == endpointId
                }
            )
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            stopSearchOperations()
            endpointNames[endpointId] = info.endpointName
            mutableState.value = mutableState.value.copy(
                phase = NearbyPhase.AWAITING_CONFIRMATION,
                pendingConnection = PendingConnection(
                    endpoint = NearbyEndpoint(endpointId, info.endpointName),
                    authenticationDigits = info.authenticationDigits
                ),
                errorMessage = null
            )
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    val endpoint = NearbyEndpoint(
                        endpointId,
                        endpointNames[endpointId] ?: "Other player"
                    )
                    stopSearchOperations()
                    mutableState.value = mutableState.value.copy(
                        phase = NearbyPhase.CONNECTED,
                        pendingConnection = null,
                        connectedEndpoint = endpoint,
                        discoveredEndpoints = emptyList(),
                        game = LocalGameState(),
                        errorMessage = null
                    )
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    mutableState.value = mutableState.value.copy(
                        phase = NearbyPhase.ERROR,
                        pendingConnection = null,
                        errorMessage = "The other player rejected the connection."
                    )
                }
                else -> mutableState.value = mutableState.value.copy(
                    phase = NearbyPhase.ERROR,
                    pendingConnection = null,
                    errorMessage = "The nearby connection could not be established."
                )
            }
        }

        override fun onDisconnected(endpointId: String) {
            endpointNames.remove(endpointId)
            timerJob?.cancel()
            clearTransferFiles()
            mutableState.value = mutableState.value.copy(
                phase = NearbyPhase.ERROR,
                connectedEndpoint = null,
                errorMessage = "The other player disconnected."
            )
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> payload.asBytes()
                    ?.let(LocalGameProtocol::decode)
                    ?.let(::handleControlMessage)
                Payload.Type.FILE -> {
                    incomingPayloads[payload.id] = payload
                    processIncomingPhoto(payload.id)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.payloadId in outgoingPayloads &&
                update.status in setOf(
                    PayloadTransferUpdate.Status.SUCCESS,
                    PayloadTransferUpdate.Status.FAILURE,
                    PayloadTransferUpdate.Status.CANCELED
                )
            ) {
                outgoingPayloads.remove(update.payloadId)?.let { (payload, file) ->
                    payload.close()
                    file.delete()
                }
            }
            if (update.payloadId in incomingPayloads) {
                val progress = if (update.totalBytes <= 0) 0f else {
                    update.bytesTransferred.toFloat() / update.totalBytes
                }
                updateGame { it.copy(transferProgress = progress.coerceIn(0f, 1f)) }
                when (update.status) {
                    PayloadTransferUpdate.Status.SUCCESS -> {
                        completedIncomingPayloads += update.payloadId
                        processIncomingPhoto(update.payloadId)
                    }
                    PayloadTransferUpdate.Status.FAILURE,
                    PayloadTransferUpdate.Status.CANCELED -> showGameError(
                        "The photo transfer did not complete.",
                        notifyOtherPlayer = true
                    )
                }
            }
        }
    }

    private companion object {
        const val MAX_PHOTOS_PER_COUNTRY = 2
    }
}
