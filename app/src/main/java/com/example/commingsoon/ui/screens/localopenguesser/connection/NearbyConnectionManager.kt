package com.example.commingsoon.ui.screens.localopenguesser.connection

import android.content.Context
import androidx.annotation.StringRes
import com.example.commingsoon.R
import com.example.commingsoon.ui.screens.localopenguesser.IndexedPhoto
import com.example.commingsoon.ui.screens.localopenguesser.OfflineCountryResolver
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
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

internal class NearbyConnectionManager(context: Context) {
    private val appContext = context.applicationContext
    private val client: ConnectionsClient = Nearby.getConnectionsClient(appContext)
    private val serviceId = "${appContext.packageName}.localopenguesser.v2"
    private val strategy = Strategy.P2P_POINT_TO_POINT
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val countryResolver by lazy { OfflineCountryResolver.load(appContext) }
    private val endpointNames = mutableMapOf<String, String>()
    private val mutableState = MutableStateFlow(NearbyConnectionState())
    val state: StateFlow<NearbyConnectionState> = mutableState.asStateFlow()

    private var selectedPhotos: List<IndexedPhoto> = emptyList()
    private var timerJob: Job? = null
    private var receivedOtherPhoto = false
    private var otherPlayerReceivedOurPhoto = false
    private var localRoundSubmission: RoundSubmission? = null
    private var remoteRoundSubmission: RoundSubmission? = null
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
        val localName = mutableState.value.localName.ifBlank {
            appContext.getString(R.string.local_guesser_default_host_name)
        }
        stopSearchOperations()
        mutableState.value = mutableState.value.copy(
            phase = NearbyPhase.ADVERTISING,
            role = NearbyRole.HOST,
            discoveredEndpoints = emptyList(),
            errorMessage = null
        )
        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        client.startAdvertising(localName, serviceId, connectionLifecycleCallback, options)
            .addOnFailureListener {
                showConnectionError(R.string.local_guesser_error_host_game, it)
            }
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
            .addOnFailureListener {
                showConnectionError(R.string.local_guesser_error_search_games, it)
            }
    }

    fun requestConnection(endpoint: NearbyEndpoint) {
        val localName = mutableState.value.localName.ifBlank {
            appContext.getString(R.string.local_guesser_default_player_name)
        }
        client.stopDiscovery()
        mutableState.value = mutableState.value.copy(
            phase = NearbyPhase.REQUESTING_CONNECTION,
            errorMessage = null
        )
        client.requestConnection(localName, endpoint.id, connectionLifecycleCallback)
            .addOnFailureListener {
                showConnectionError(R.string.local_guesser_error_request_connection, it)
            }
    }

    fun acceptPendingConnection() {
        val pending = mutableState.value.pendingConnection ?: return
        mutableState.value = mutableState.value.copy(
            phase = NearbyPhase.CONNECTING,
            pendingConnection = null
        )
        client.acceptConnection(pending.endpoint.id, payloadCallback)
            .addOnFailureListener {
                showConnectionError(R.string.local_guesser_error_accept_connection, it)
            }
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
                    "roundSeconds" to safeSettings.roundSeconds,
                    "homePhotoExclusionMode" to safeSettings.homePhotoExclusionMode.name
                )
            )
            updateGame {
                it.copy(
                    phase = LocalGamePhase.WAITING_FOR_OTHER_PLAYER,
                    statusMessage = message(
                        R.string.local_guesser_status_waiting_phone_prepare
                    )
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
        ).addOnFailureListener {
            showConnectionError(R.string.local_guesser_error_send_test_message, it)
        }
    }

    fun setGuess(latitude: Double, longitude: Double) {
        if (mutableState.value.game.phase != LocalGamePhase.PLAYING_ROUND) return
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return
        updateGame { it.copy(currentGuess = GuessLocation(latitude, longitude)) }
    }

    fun continueAfterRound() {
        if (mutableState.value.role != NearbyRole.HOST ||
            mutableState.value.game.phase != LocalGamePhase.ROUND_RESULT
        ) return
        startRound(mutableState.value.game.currentRound + 1)
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
                statusMessage = message(R.string.local_guesser_status_selecting_photos)
            )
        }
        scope.launch {
            val selection = withContext(Dispatchers.IO) { selectRandomPhotos(settings) }
            if (selection.size != settings.roundCount) {
                showGameError(
                    message(
                        R.string.local_guesser_error_not_enough_photos,
                        settings.roundCount
                    ),
                    notifyOtherPlayer = true
                )
                return@launch
            }
            selectedPhotos = selection
            onReady()
            if (notifyHostWhenReady) {
                updateGame {
                    it.copy(
                        phase = LocalGamePhase.WAITING_FOR_OTHER_PLAYER,
                        statusMessage = message(
                            R.string.local_guesser_status_photos_selected
                        )
                    )
                }
                sendControl(LocalGameProtocol.TYPE_PLAYER_READY)
            }
        }
    }

    private fun selectRandomPhotos(settings: LocalGameSettings): List<IndexedPhoto> {
        val database = PhotoIndexDatabase(appContext)
        val eligiblePhotos = try {
            database.readAll().values.filter {
                !it.unreadable && it.latitude != null && it.longitude != null && it.country != null
            }
        } finally {
            database.close()
        }
        val excludedMediaIds = homePhotosToExclude(
            eligiblePhotos,
            settings.homePhotoExclusionMode
        )
        val candidates = eligiblePhotos.filterNot { it.mediaId in excludedMediaIds }.shuffled()
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
        }.take(settings.roundCount)
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
            showGameError(
                message(R.string.local_guesser_error_round_missing_photo, round + 1),
                notifyOtherPlayer = true
            )
            return
        }
        timerJob?.cancel()
        receivedOtherPhoto = false
        otherPlayerReceivedOurPhoto = false
        localRoundSubmission = null
        remoteRoundSubmission = null
        mutableState.value.game.receivedPhotoPath?.let { File(it).delete() }
        updateGame {
            it.copy(
                phase = LocalGamePhase.TRANSFERRING_PHOTO,
                currentRound = round,
                receivedPhotoPath = null,
                transferProgress = 0f,
                secondsRemaining = it.settings.roundSeconds,
                currentGuess = null,
                currentRoundResult = null,
                canContinueAfterRound = false,
                statusMessage = message(
                    R.string.local_guesser_status_exchanging_photos,
                    round + 1
                )
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
                    showGameError(
                        message(R.string.local_guesser_error_send_round_photo),
                        notifyOtherPlayer = true
                    )
                }
            }.onFailure {
                showGameError(
                    message(R.string.local_guesser_error_prepare_round_photo),
                    notifyOtherPlayer = true
                )
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
            submitRoundGuess()
        }
    }

    private fun submitRoundGuess() {
        val game = mutableState.value.game
        val photo = selectedPhotos.getOrNull(game.currentRound) ?: return
        val actual = GuessLocation(
            latitude = photo.latitude ?: return,
            longitude = photo.longitude ?: return
        )
        val submission = RoundSubmission(game.currentRound, game.currentGuess, actual)
        localRoundSubmission = submission
        val values = mutableMapOf<String, Any?>(
            "round" to submission.round,
            "hasGuess" to (submission.guess != null),
            "actualLat" to submission.actual.latitude,
            "actualLon" to submission.actual.longitude
        )
        submission.guess?.let {
            values["guessLat"] = it.latitude
            values["guessLon"] = it.longitude
        }
        sendControl(LocalGameProtocol.TYPE_ROUND_GUESS, values)
        updateGame {
            it.copy(statusMessage = message(R.string.local_guesser_status_waiting_result))
        }
        maybeFinalizeRound()
    }

    private fun maybeFinalizeRound() {
        if (mutableState.value.role != NearbyRole.HOST) return
        val host = localRoundSubmission ?: return
        val joiner = remoteRoundSubmission ?: return
        if (host.round != joiner.round || host.round != mutableState.value.game.currentRound) return

        val hostDistance = host.guess?.let { localGuesserDistanceKm(it, joiner.actual) }
        val joinerDistance = joiner.guess?.let { localGuesserDistanceKm(it, host.actual) }
        val result = CanonicalRoundResult(
            round = host.round,
            hostGuess = host.guess,
            joinerGuess = joiner.guess,
            hostActual = host.actual,
            joinerActual = joiner.actual,
            hostDistanceKm = hostDistance,
            joinerDistanceKm = joinerDistance,
            hostPoints = localGuesserPoints(
                hostDistance,
                correctCountry = isSameCountry(host.guess, joiner.actual)
            ),
            joinerPoints = localGuesserPoints(
                joinerDistance,
                correctCountry = isSameCountry(joiner.guess, host.actual)
            )
        )
        sendControl(LocalGameProtocol.TYPE_ROUND_RESULT, result.toValues())
        applyRoundResult(result)
    }

    private fun isSameCountry(guess: GuessLocation?, actual: GuessLocation): Boolean {
        val guessedCountry = guess?.let { countryResolver.countryAt(it.latitude, it.longitude) }
            ?: return false
        val actualCountry = countryResolver.countryAt(actual.latitude, actual.longitude)
            ?: return false
        return guessedCountry == actualCountry
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
                statusMessage = message(
                    R.string.local_guesser_status_all_rounds_complete,
                    it.settings.roundCount
                )
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
                    roundSeconds = json.optInt("roundSeconds").coerceIn(10, 300),
                    homePhotoExclusionMode = runCatching {
                        HomePhotoExclusionMode.valueOf(
                            json.optString(
                                "homePhotoExclusionMode",
                                HomePhotoExclusionMode.NONE.name
                            )
                        )
                    }.getOrDefault(HomePhotoExclusionMode.NONE)
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
            LocalGameProtocol.TYPE_ROUND_GUESS -> {
                val submission = json.toRoundSubmission() ?: return
                if (submission.round == mutableState.value.game.currentRound) {
                    remoteRoundSubmission = submission
                    maybeFinalizeRound()
                }
            }
            LocalGameProtocol.TYPE_ROUND_RESULT -> json.toCanonicalRoundResult()?.let { result ->
                applyRoundResult(result)
                if (mutableState.value.role == NearbyRole.JOINER) {
                    sendControl(
                        LocalGameProtocol.TYPE_ROUND_RESULT_READY,
                        mapOf("round" to result.round)
                    )
                }
            }
            LocalGameProtocol.TYPE_ROUND_RESULT_READY -> {
                if (mutableState.value.role == NearbyRole.HOST &&
                    json.optInt("round", -1) == mutableState.value.game.currentRound &&
                    mutableState.value.game.phase == LocalGamePhase.ROUND_RESULT
                ) {
                    updateGame { it.copy(canContinueAfterRound = true) }
                }
            }
            LocalGameProtocol.TYPE_GAME_FINISHED -> finishGame()
            LocalGameProtocol.TYPE_GAME_ERROR -> {
                val resourceName = json.optString("messageResource")
                val resourceId = appContext.resources.getIdentifier(
                    resourceName,
                    "string",
                    appContext.packageName
                )
                val argsJson = json.optJSONArray("messageArgs")
                val args = buildList {
                    if (argsJson != null) {
                        repeat(argsJson.length()) { index -> add(argsJson.get(index)) }
                    }
                }
                showGameError(
                    if (resourceId != 0) {
                        LocalGuesserMessage(resourceId, args)
                    } else {
                        message(R.string.local_guesser_error_other_phone_prepare)
                    },
                    notifyOtherPlayer = false
                )
            }
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
                        statusMessage = message(
                            R.string.local_guesser_status_photo_received
                        )
                    )
                }
                sendControl(LocalGameProtocol.TYPE_PHOTO_READY, mapOf("round" to round))
                maybeRevealRound()
            }.onFailure {
                processingPayloads.remove(payloadId)
                showGameError(
                    message(R.string.local_guesser_error_save_received_photo),
                    notifyOtherPlayer = true
                )
            }
        }
    }

    private fun applyRoundResult(result: CanonicalRoundResult) {
        if (result.round != mutableState.value.game.currentRound) return
        val isHost = mutableState.value.role == NearbyRole.HOST
        val roundResult = if (isHost) {
            RoundResult(
                round = result.round,
                localGuess = result.hostGuess,
                actualLocation = result.joinerActual,
                localDistanceKm = result.hostDistanceKm,
                localPoints = result.hostPoints,
                opponentDistanceKm = result.joinerDistanceKm,
                opponentPoints = result.joinerPoints
            )
        } else {
            RoundResult(
                round = result.round,
                localGuess = result.joinerGuess,
                actualLocation = result.hostActual,
                localDistanceKm = result.joinerDistanceKm,
                localPoints = result.joinerPoints,
                opponentDistanceKm = result.hostDistanceKm,
                opponentPoints = result.hostPoints
            )
        }
        updateGame { game ->
            val results = (game.roundResults.filterNot { it.round == roundResult.round } + roundResult)
                .sortedBy(RoundResult::round)
            game.copy(
                phase = LocalGamePhase.ROUND_RESULT,
                currentRoundResult = roundResult,
                roundResults = results,
                canContinueAfterRound = !isHost,
                secondsRemaining = 0,
                statusMessage = null
            )
        }
    }

    private fun sendControl(type: String, values: Map<String, Any?> = emptyMap()) {
        val endpoint = mutableState.value.connectedEndpoint ?: return
        client.sendPayload(endpoint.id, Payload.fromBytes(LocalGameProtocol.encode(type, values)))
            .addOnFailureListener {
                showGameError(
                    message(R.string.local_guesser_error_sync_game),
                    notifyOtherPlayer = false
                )
            }
    }

    private fun showGameError(message: LocalGuesserMessage, notifyOtherPlayer: Boolean) {
        timerJob?.cancel()
        if (notifyOtherPlayer) {
            sendControl(
                LocalGameProtocol.TYPE_GAME_ERROR,
                mapOf(
                    "messageResource" to appContext.resources
                        .getResourceEntryName(message.resourceId),
                    "messageArgs" to message.args
                )
            )
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

    private fun showConnectionError(@StringRes messageId: Int, error: Exception) {
        stopSearchOperations()
        mutableState.value = mutableState.value.copy(
            phase = NearbyPhase.ERROR,
            pendingConnection = null,
            errorMessage = message(
                messageId,
                error.localizedMessage ?: error.javaClass.simpleName
            )
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
                        endpointNames[endpointId] ?: appContext.getString(
                            R.string.local_guesser_other_player
                        )
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
                        errorMessage = message(
                            R.string.local_guesser_error_connection_rejected
                        )
                    )
                }
                else -> mutableState.value = mutableState.value.copy(
                    phase = NearbyPhase.ERROR,
                    pendingConnection = null,
                    errorMessage = message(
                        R.string.local_guesser_error_connection_failed
                    )
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
                errorMessage = message(R.string.local_guesser_error_player_disconnected)
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
                        message(R.string.local_guesser_error_photo_transfer),
                        notifyOtherPlayer = true
                    )
                }
            }
        }
    }

    private companion object {
        const val MAX_PHOTOS_PER_COUNTRY = 2
    }

    private fun message(@StringRes resourceId: Int, vararg args: Any) =
        LocalGuesserMessage(resourceId, args.toList())
}

private data class RoundSubmission(
    val round: Int,
    val guess: GuessLocation?,
    val actual: GuessLocation
)

private data class CanonicalRoundResult(
    val round: Int,
    val hostGuess: GuessLocation?,
    val joinerGuess: GuessLocation?,
    val hostActual: GuessLocation,
    val joinerActual: GuessLocation,
    val hostDistanceKm: Double?,
    val joinerDistanceKm: Double?,
    val hostPoints: Int,
    val joinerPoints: Int
) {
    fun toValues(): Map<String, Any?> = buildMap {
        put("round", round)
        put("hostHasGuess", hostGuess != null)
        put("joinerHasGuess", joinerGuess != null)
        put("hostActualLat", hostActual.latitude)
        put("hostActualLon", hostActual.longitude)
        put("joinerActualLat", joinerActual.latitude)
        put("joinerActualLon", joinerActual.longitude)
        put("hostDistanceKm", hostDistanceKm ?: -1.0)
        put("joinerDistanceKm", joinerDistanceKm ?: -1.0)
        put("hostPoints", hostPoints)
        put("joinerPoints", joinerPoints)
        hostGuess?.let {
            put("hostGuessLat", it.latitude)
            put("hostGuessLon", it.longitude)
        }
        joinerGuess?.let {
            put("joinerGuessLat", it.latitude)
            put("joinerGuessLon", it.longitude)
        }
    }
}

private fun JSONObject.toRoundSubmission(): RoundSubmission? {
    val round = optInt("round", -1)
    val actualLat = optDouble("actualLat", Double.NaN)
    val actualLon = optDouble("actualLon", Double.NaN)
    if (round < 0 || !actualLat.isFinite() || !actualLon.isFinite()) return null
    val guess = if (optBoolean("hasGuess")) {
        val latitude = optDouble("guessLat", Double.NaN)
        val longitude = optDouble("guessLon", Double.NaN)
        if (!latitude.isFinite() || !longitude.isFinite()) return null
        GuessLocation(latitude, longitude)
    } else {
        null
    }
    return RoundSubmission(round, guess, GuessLocation(actualLat, actualLon))
}

private fun JSONObject.toCanonicalRoundResult(): CanonicalRoundResult? {
    val round = optInt("round", -1)
    val hostActual = location("hostActual") ?: return null
    val joinerActual = location("joinerActual") ?: return null
    if (round < 0) return null
    return CanonicalRoundResult(
        round = round,
        hostGuess = optionalLocation("hostGuess", "hostHasGuess") ?: if (optBoolean("hostHasGuess")) return null else null,
        joinerGuess = optionalLocation("joinerGuess", "joinerHasGuess") ?: if (optBoolean("joinerHasGuess")) return null else null,
        hostActual = hostActual,
        joinerActual = joinerActual,
        hostDistanceKm = optDouble("hostDistanceKm", -1.0).takeIf { it >= 0.0 },
        joinerDistanceKm = optDouble("joinerDistanceKm", -1.0).takeIf { it >= 0.0 },
        hostPoints = optInt("hostPoints").coerceIn(0, MAX_ROUND_POINTS),
        joinerPoints = optInt("joinerPoints").coerceIn(0, MAX_ROUND_POINTS)
    )
}

private fun JSONObject.location(prefix: String): GuessLocation? {
    val latitude = optDouble("${prefix}Lat", Double.NaN)
    val longitude = optDouble("${prefix}Lon", Double.NaN)
    return if (latitude.isFinite() && longitude.isFinite()) GuessLocation(latitude, longitude) else null
}

private fun JSONObject.optionalLocation(prefix: String, presentKey: String): GuessLocation? =
    if (optBoolean(presentKey)) location(prefix) else null

internal fun localGuesserDistanceKm(first: GuessLocation, second: GuessLocation): Double {
    val lat1 = Math.toRadians(first.latitude)
    val lat2 = Math.toRadians(second.latitude)
    val deltaLat = lat2 - lat1
    val deltaLon = Math.toRadians(second.longitude - first.longitude)
    val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
        cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2)
    return 2 * EARTH_RADIUS_KM * asin(sqrt(a.coerceIn(0.0, 1.0)))
}

internal fun localGuesserPoints(distanceKm: Double?, correctCountry: Boolean = false): Int {
    if (distanceKm == null) return 0
    val distancePoints = if (distanceKm <= MAX_SCORE_DISTANCE_KM) {
        MAX_ROUND_POINTS
    } else {
        val normalizedDistance =
            (distanceKm - MAX_SCORE_DISTANCE_KM) / (LONG_DISTANCE_REFERENCE_KM - MAX_SCORE_DISTANCE_KM)
        val decay = ln(MAX_ROUND_POINTS.toDouble() / LONG_DISTANCE_REFERENCE_POINTS)
        (MAX_ROUND_POINTS * exp(-decay * sqrt(normalizedDistance)))
            .roundToInt()
            .coerceIn(0, MAX_ROUND_POINTS)
    }
    val countryBonus = if (correctCountry) CORRECT_COUNTRY_BONUS else 0
    return (distancePoints + countryBonus).coerceAtMost(MAX_ROUND_POINTS)
}

private const val EARTH_RADIUS_KM = 6_371.0088
private const val MAX_ROUND_POINTS = 5_000
private const val CORRECT_COUNTRY_BONUS = 1_000
private const val MAX_SCORE_DISTANCE_KM = 5.0
private const val LONG_DISTANCE_REFERENCE_KM = 300.0
private const val LONG_DISTANCE_REFERENCE_POINTS = 2_000
