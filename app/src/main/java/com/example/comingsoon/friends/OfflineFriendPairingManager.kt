package com.example.comingsoon.friends

import android.content.Context
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
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID
import com.example.comingsoon.R
import com.example.comingsoon.language.localizedString

enum class OfflinePairingPhase {
    IDLE,
    ADVERTISING,
    DISCOVERING,
    REQUESTING,
    AWAITING_CONFIRMATION,
    CONNECTING,
    EXCHANGING_PROFILE,
    TRANSFERRING_JOURNEY,
    JOURNEY_SHARED,
    JOURNEY_RECEIVED,
    PAIRED,
    ERROR
}

enum class OfflinePairingMode {
    FRIEND_PAIRING,
    JOURNEY_SHARING,
    JOURNEY_RECEIVING
}

data class OfflineFriendEndpoint(
    val id: String,
    val name: String
)

data class OfflinePendingConnection(
    val endpoint: OfflineFriendEndpoint,
    val authenticationDigits: String
)

data class OfflineFriendPairingState(
    val phase: OfflinePairingPhase = OfflinePairingPhase.IDLE,
    val mode: OfflinePairingMode = OfflinePairingMode.FRIEND_PAIRING,
    val discoveredEndpoints: List<OfflineFriendEndpoint> = emptyList(),
    val pendingConnection: OfflinePendingConnection? = null,
    val pairedFriendName: String? = null,
    val journeyTitle: String? = null,
    val targetFriendName: String? = null,
    val errorMessage: String? = null
)

class OfflineFriendPairingManager(
    context: Context,
    private val ownIdentity: () -> OfflineFriendIdentity,
    private val onFriendReceived: suspend (OfflineFriendIdentity, String) -> Unit,
    private val onJourneyReceived: suspend (
        OfflineFriendIdentity,
        OfflineJourneyPayload
    ) -> Unit,
    private val onJourneySent: suspend (
        OfflineFriendIdentity,
        OfflineJourneyPayload
    ) -> Unit
) {
    private val appContext = context.applicationContext
    private val client: ConnectionsClient = Nearby.getConnectionsClient(appContext)
    private val gson = Gson()
    private val serviceId = "${appContext.packageName}.offlinefriends.v1"
    private val strategy = Strategy.P2P_POINT_TO_POINT
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val endpointNames = mutableMapOf<String, String>()
    private val endpointIdentities = mutableMapOf<String, OfflineFriendIdentity>()
    private val mutableState = MutableStateFlow(OfflineFriendPairingState())
    val state: StateFlow<OfflineFriendPairingState> = mutableState.asStateFlow()
    private var pairingNonce = UUID.randomUUID().toString()
    private var pendingJourneyShare: PendingJourneyShare? = null

    fun hasGooglePlayServices(): Boolean = GoogleApiAvailability.getInstance()
        .isGooglePlayServicesAvailable(appContext) == ConnectionResult.SUCCESS

    fun startAdvertising() {
        resetConnections()
        pairingNonce = UUID.randomUUID().toString()
        mutableState.value = OfflineFriendPairingState(
            phase = OfflinePairingPhase.ADVERTISING
        )
        val name = ownIdentity().name.take(32)
        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        client.startAdvertising(name, serviceId, connectionLifecycleCallback, options)
            .addOnFailureListener {
                fail(appContext.localizedString(R.string.offline_friend_visible_failed))
            }
    }

    fun startDiscovery() {
        resetConnections()
        pairingNonce = UUID.randomUUID().toString()
        mutableState.value = OfflineFriendPairingState(
            phase = OfflinePairingPhase.DISCOVERING
        )
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        client.startDiscovery(serviceId, endpointDiscoveryCallback, options)
            .addOnFailureListener {
                fail(appContext.localizedString(R.string.offline_friend_search_failed))
            }
    }

    fun startJourneyShare(
        targetIdentityKey: String,
        targetFriendName: String,
        journey: OfflineJourneyPayload
    ) {
        resetConnections()
        pendingJourneyShare = PendingJourneyShare(targetIdentityKey, journey)
        pairingNonce = UUID.randomUUID().toString()
        mutableState.value = OfflineFriendPairingState(
            phase = OfflinePairingPhase.DISCOVERING,
            mode = OfflinePairingMode.JOURNEY_SHARING,
            journeyTitle = journey.title,
            targetFriendName = targetFriendName
        )
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        client.startDiscovery(serviceId, endpointDiscoveryCallback, options)
            .addOnFailureListener {
                fail(appContext.localizedString(R.string.offline_journey_search_failed))
            }
    }

    fun requestConnection(endpoint: OfflineFriendEndpoint) {
        client.stopDiscovery()
        mutableState.value = mutableState.value.copy(
            phase = OfflinePairingPhase.REQUESTING,
            errorMessage = null
        )
        client.requestConnection(
            ownIdentity().name.take(32),
            endpoint.id,
            connectionLifecycleCallback
        ).addOnFailureListener {
            fail(appContext.localizedString(R.string.offline_friend_request_failed))
        }
    }

    fun acceptPendingConnection() {
        val pending = mutableState.value.pendingConnection ?: return
        mutableState.value = mutableState.value.copy(
            phase = OfflinePairingPhase.CONNECTING,
            pendingConnection = null
        )
        client.acceptConnection(pending.endpoint.id, payloadCallback)
            .addOnFailureListener {
                fail(appContext.localizedString(R.string.offline_friend_confirm_failed))
            }
    }

    fun rejectPendingConnection() {
        val pending = mutableState.value.pendingConnection ?: return
        client.rejectConnection(pending.endpoint.id)
        stop()
    }

    fun stop() {
        resetConnections()
        pendingJourneyShare = null
        mutableState.value = OfflineFriendPairingState()
    }

    fun close() {
        resetConnections()
        scope.cancel()
    }

    private fun sendIdentity(endpointId: String) {
        val envelope = OfflineEnvelope(
            version = PROTOCOL_VERSION,
            type = PROFILE_TYPE,
            pairingNonce = pairingNonce,
            identity = ownIdentity()
        )
        val bytes = gson.toJson(envelope).toByteArray(Charsets.UTF_8)
        client.sendPayload(endpointId, Payload.fromBytes(bytes))
            .addOnFailureListener {
                fail(appContext.localizedString(R.string.offline_friend_transfer_failed))
            }
    }

    private fun receiveIdentity(endpointId: String, envelope: OfflineEnvelope) {
        val identity = envelope.identity
        if (
            envelope.version != PROTOCOL_VERSION ||
            envelope.type != PROFILE_TYPE ||
            identity == null ||
            envelope.pairingNonce.isNullOrBlank() ||
            identity.deviceId.isBlank() ||
            identity.name.isBlank()
        ) {
            fail(appContext.localizedString(R.string.offline_friend_invalid_profile))
            return
        }
        endpointIdentities[endpointId] = identity
        val pairingId = offlinePairingId(
            pairingNonce,
            requireNotNull(envelope.pairingNonce)
        )

        scope.launch {
            runCatching { onFriendReceived(identity, pairingId) }
                .onSuccess {
                    stopSearchOperations()
                    val outgoing = pendingJourneyShare
                    if (outgoing == null) {
                        mutableState.value = mutableState.value.copy(
                            phase = OfflinePairingPhase.PAIRED,
                            pendingConnection = null,
                            pairedFriendName = identity.name,
                            errorMessage = null
                        )
                    } else if (identity.identityKey != outgoing.targetIdentityKey) {
                        fail(
                            appContext.localizedString(
                                R.string.offline_journey_wrong_friend,
                                mutableState.value.targetFriendName.orEmpty()
                            )
                        )
                    } else {
                        mutableState.value = mutableState.value.copy(
                            phase = OfflinePairingPhase.TRANSFERRING_JOURNEY,
                            pendingConnection = null,
                            pairedFriendName = identity.name,
                            errorMessage = null
                        )
                        sendJourney(endpointId, outgoing.payload)
                    }
                }
                .onFailure {
                    fail(appContext.localizedString(R.string.offline_friend_save_failed))
                }
        }
    }

    private fun sendJourney(endpointId: String, journey: OfflineJourneyPayload) {
        val bytes = gson.toJson(
            OfflineEnvelope(
                version = PROTOCOL_VERSION,
                type = JOURNEY_TYPE,
                journey = journey
            )
        ).toByteArray(Charsets.UTF_8)
        if (bytes.size > MAX_BYTES_PAYLOAD) {
            fail(appContext.localizedString(R.string.offline_journey_too_large))
            return
        }
        client.sendPayload(endpointId, Payload.fromBytes(bytes))
            .addOnFailureListener {
                fail(appContext.localizedString(R.string.offline_journey_transfer_failed))
            }
    }

    private fun receiveJourney(endpointId: String, envelope: OfflineEnvelope) {
        val owner = endpointIdentities[endpointId]
        val journey = envelope.journey
        val startDate = journey?.startDate?.let {
            runCatching { java.time.LocalDate.parse(it) }.getOrNull()
        }
        val endDate = journey?.endDate?.let {
            runCatching { java.time.LocalDate.parse(it) }.getOrNull()
        }
        if (
            owner == null ||
            journey == null ||
            journey.transferId.isBlank() ||
            journey.title.isBlank() ||
            journey.title.length > 200 ||
            startDate == null ||
            endDate == null ||
            endDate < startDate ||
            journey.locations.size > 500 ||
            journey.visitedCountries.size > 300 ||
            journey.locations.any {
                it.name.length > 200 ||
                    it.latitude !in -90.0..90.0 ||
                    it.longitude !in -180.0..180.0
            }
        ) {
            fail(appContext.localizedString(R.string.offline_journey_invalid))
            return
        }
        scope.launch {
            runCatching { onJourneyReceived(owner, journey) }
                .onSuccess {
                    val acknowledgement = OfflineEnvelope(
                        version = PROTOCOL_VERSION,
                        type = JOURNEY_ACK_TYPE,
                        transferId = journey.transferId
                    )
                    client.sendPayload(
                        endpointId,
                        Payload.fromBytes(
                            gson.toJson(acknowledgement).toByteArray(Charsets.UTF_8)
                        )
                    )
                    mutableState.value = mutableState.value.copy(
                        phase = OfflinePairingPhase.JOURNEY_RECEIVED,
                        mode = OfflinePairingMode.JOURNEY_RECEIVING,
                        pairedFriendName = owner.name,
                        journeyTitle = journey.title,
                        errorMessage = null
                    )
                }
                .onFailure {
                    fail(appContext.localizedString(R.string.offline_journey_save_failed))
                }
        }
    }

    private fun receiveJourneyAcknowledgement(endpointId: String, envelope: OfflineEnvelope) {
        val recipient = endpointIdentities[endpointId] ?: return
        val pending = pendingJourneyShare ?: return
        if (envelope.transferId != pending.payload.transferId) return
        scope.launch {
            runCatching { onJourneySent(recipient, pending.payload) }
                .onSuccess {
                    pendingJourneyShare = null
                    stopSearchOperations()
                    mutableState.value = mutableState.value.copy(
                        phase = OfflinePairingPhase.JOURNEY_SHARED,
                        pairedFriendName = recipient.name,
                        errorMessage = null
                    )
                }
                .onFailure {
                    fail(appContext.localizedString(R.string.offline_journey_save_failed))
                }
        }
    }

    private fun resetConnections() {
        stopSearchOperations()
        client.stopAllEndpoints()
        endpointNames.clear()
        endpointIdentities.clear()
    }

    private fun stopSearchOperations() {
        client.stopAdvertising()
        client.stopDiscovery()
    }

    private fun fail(message: String) {
        val previous = mutableState.value
        resetConnections()
        mutableState.value = OfflineFriendPairingState(
            phase = OfflinePairingPhase.ERROR,
            mode = previous.mode,
            pairedFriendName = previous.pairedFriendName,
            journeyTitle = previous.journeyTitle,
            targetFriendName = previous.targetFriendName,
            errorMessage = message
        )
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            endpointNames[endpointId] = info.endpointName
            val endpoint = OfflineFriendEndpoint(endpointId, info.endpointName)
            mutableState.value = mutableState.value.copy(
                discoveredEndpoints = mutableState.value.discoveredEndpoints
                    .filterNot { it.id == endpointId } + endpoint
            )
        }

        override fun onEndpointLost(endpointId: String) {
            endpointNames.remove(endpointId)
            mutableState.value = mutableState.value.copy(
                discoveredEndpoints = mutableState.value.discoveredEndpoints
                    .filterNot { it.id == endpointId }
            )
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            stopSearchOperations()
            endpointNames[endpointId] = info.endpointName
            mutableState.value = mutableState.value.copy(
                phase = OfflinePairingPhase.AWAITING_CONFIRMATION,
                pendingConnection = OfflinePendingConnection(
                    endpoint = OfflineFriendEndpoint(endpointId, info.endpointName),
                    authenticationDigits = info.authenticationDigits
                ),
                errorMessage = null
            )
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    mutableState.value = mutableState.value.copy(
                        phase = OfflinePairingPhase.EXCHANGING_PROFILE,
                        pendingConnection = null,
                        errorMessage = null
                    )
                    sendIdentity(endpointId)
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED ->
                    fail(appContext.localizedString(R.string.offline_friend_rejected))
                else -> fail(
                    appContext.localizedString(R.string.offline_friend_connection_failed)
                )
            }
        }

        override fun onDisconnected(endpointId: String) {
            endpointNames.remove(endpointId)
            if (
                mutableState.value.phase == OfflinePairingPhase.CONNECTING ||
                mutableState.value.phase == OfflinePairingPhase.EXCHANGING_PROFILE ||
                mutableState.value.phase == OfflinePairingPhase.TRANSFERRING_JOURNEY
            ) {
                fail(appContext.localizedString(R.string.offline_friend_disconnected))
            }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val envelope = payload.asBytes()?.let { bytes ->
                    runCatching {
                        gson.fromJson(
                            String(bytes, Charsets.UTF_8),
                            OfflineEnvelope::class.java
                        )
                    }.getOrNull()
                } ?: run {
                    fail(appContext.localizedString(R.string.offline_friend_invalid_profile))
                    return
                }
                if (envelope.version != PROTOCOL_VERSION) {
                    fail(appContext.localizedString(R.string.offline_friend_invalid_profile))
                    return
                }
                when (envelope.type) {
                    PROFILE_TYPE -> receiveIdentity(endpointId, envelope)
                    JOURNEY_TYPE -> receiveJourney(endpointId, envelope)
                    JOURNEY_ACK_TYPE -> receiveJourneyAcknowledgement(endpointId, envelope)
                    else -> fail(appContext.localizedString(R.string.offline_friend_invalid_profile))
                }
            }
        }

        override fun onPayloadTransferUpdate(
            endpointId: String,
            update: PayloadTransferUpdate
        ) = Unit
    }

    private data class OfflineEnvelope(
        val version: Int,
        val type: String,
        val pairingNonce: String? = null,
        val identity: OfflineFriendIdentity? = null,
        val journey: OfflineJourneyPayload? = null,
        val transferId: String? = null
    )

    private data class PendingJourneyShare(
        val targetIdentityKey: String,
        val payload: OfflineJourneyPayload
    )

    private companion object {
        const val PROTOCOL_VERSION = 2
        const val PROFILE_TYPE = "friend_profile"
        const val JOURNEY_TYPE = "journey"
        const val JOURNEY_ACK_TYPE = "journey_ack"
        const val MAX_BYTES_PAYLOAD = 32 * 1024
    }
}

internal fun offlinePairingId(firstNonce: String, secondNonce: String): String {
    val canonical = listOf(firstNonce, secondNonce).sorted().joinToString("|")
    return MessageDigest.getInstance("SHA-256")
        .digest(canonical.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
