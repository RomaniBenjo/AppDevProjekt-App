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

enum class OfflinePairingPhase {
    IDLE,
    ADVERTISING,
    DISCOVERING,
    REQUESTING,
    AWAITING_CONFIRMATION,
    CONNECTING,
    EXCHANGING_PROFILE,
    PAIRED,
    ERROR
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
    val discoveredEndpoints: List<OfflineFriendEndpoint> = emptyList(),
    val pendingConnection: OfflinePendingConnection? = null,
    val pairedFriendName: String? = null,
    val errorMessage: String? = null
)

class OfflineFriendPairingManager(
    context: Context,
    private val ownIdentity: () -> OfflineFriendIdentity,
    private val onFriendReceived: suspend (OfflineFriendIdentity, String) -> Unit
) {
    private val appContext = context.applicationContext
    private val client: ConnectionsClient = Nearby.getConnectionsClient(appContext)
    private val gson = Gson()
    private val serviceId = "${appContext.packageName}.offlinefriends.v1"
    private val strategy = Strategy.P2P_POINT_TO_POINT
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val endpointNames = mutableMapOf<String, String>()
    private val mutableState = MutableStateFlow(OfflineFriendPairingState())
    val state: StateFlow<OfflineFriendPairingState> = mutableState.asStateFlow()
    private var pairingNonce = UUID.randomUUID().toString()

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
            .addOnFailureListener { fail("Dieses Gerät konnte nicht sichtbar gemacht werden.", it) }
    }

    fun startDiscovery() {
        resetConnections()
        pairingNonce = UUID.randomUUID().toString()
        mutableState.value = OfflineFriendPairingState(
            phase = OfflinePairingPhase.DISCOVERING
        )
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        client.startDiscovery(serviceId, endpointDiscoveryCallback, options)
            .addOnFailureListener { fail("Geräte in der Nähe konnten nicht gesucht werden.", it) }
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
        ).addOnFailureListener { fail("Die Verbindung konnte nicht angefordert werden.", it) }
    }

    fun acceptPendingConnection() {
        val pending = mutableState.value.pendingConnection ?: return
        mutableState.value = mutableState.value.copy(
            phase = OfflinePairingPhase.CONNECTING,
            pendingConnection = null
        )
        client.acceptConnection(pending.endpoint.id, payloadCallback)
            .addOnFailureListener { fail("Die Verbindung konnte nicht bestätigt werden.", it) }
    }

    fun rejectPendingConnection() {
        val pending = mutableState.value.pendingConnection ?: return
        client.rejectConnection(pending.endpoint.id)
        stop()
    }

    fun stop() {
        resetConnections()
        mutableState.value = OfflineFriendPairingState()
    }

    fun close() {
        resetConnections()
        scope.cancel()
    }

    private fun sendIdentity(endpointId: String) {
        val envelope = OfflineProfileEnvelope(
            version = PROTOCOL_VERSION,
            type = PROFILE_TYPE,
            pairingNonce = pairingNonce,
            identity = ownIdentity()
        )
        val bytes = gson.toJson(envelope).toByteArray(Charsets.UTF_8)
        client.sendPayload(endpointId, Payload.fromBytes(bytes))
            .addOnFailureListener { fail("Das Offline-Profil konnte nicht übertragen werden.", it) }
    }

    private fun receiveIdentity(bytes: ByteArray) {
        val envelope = runCatching {
            gson.fromJson(String(bytes, Charsets.UTF_8), OfflineProfileEnvelope::class.java)
        }.getOrNull()
        val identity = envelope?.identity
        if (
            envelope?.version != PROTOCOL_VERSION ||
            envelope.type != PROFILE_TYPE ||
            identity == null ||
            envelope.pairingNonce.isNullOrBlank() ||
            identity.deviceId.isBlank() ||
            identity.name.isBlank()
        ) {
            fail("Das andere Gerät hat kein gültiges Offline-Profil gesendet.")
            return
        }
        val pairingId = offlinePairingId(
            pairingNonce,
            requireNotNull(envelope.pairingNonce)
        )

        scope.launch {
            runCatching { onFriendReceived(identity, pairingId) }
                .onSuccess {
                    stopSearchOperations()
                    mutableState.value = mutableState.value.copy(
                        phase = OfflinePairingPhase.PAIRED,
                        pendingConnection = null,
                        pairedFriendName = identity.name,
                        errorMessage = null
                    )
                }
                .onFailure { fail(it.message ?: "Der Freund konnte nicht gespeichert werden.") }
        }
    }

    private fun resetConnections() {
        stopSearchOperations()
        client.stopAllEndpoints()
        endpointNames.clear()
    }

    private fun stopSearchOperations() {
        client.stopAdvertising()
        client.stopDiscovery()
    }

    private fun fail(message: String, throwable: Throwable? = null) {
        resetConnections()
        mutableState.value = OfflineFriendPairingState(
            phase = OfflinePairingPhase.ERROR,
            errorMessage = throwable?.localizedMessage?.let { "$message $it" } ?: message
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
                    fail("Die andere Person hat die Verbindung abgelehnt.")
                else -> fail("Die Verbindung in der Nähe konnte nicht hergestellt werden.")
            }
        }

        override fun onDisconnected(endpointId: String) {
            endpointNames.remove(endpointId)
            if (
                mutableState.value.phase == OfflinePairingPhase.CONNECTING ||
                mutableState.value.phase == OfflinePairingPhase.EXCHANGING_PROFILE
            ) {
                fail("Die Verbindung zum anderen Gerät wurde getrennt.")
            }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                payload.asBytes()?.let(::receiveIdentity)
            }
        }

        override fun onPayloadTransferUpdate(
            endpointId: String,
            update: PayloadTransferUpdate
        ) = Unit
    }

    private data class OfflineProfileEnvelope(
        val version: Int,
        val type: String,
        val pairingNonce: String?,
        val identity: OfflineFriendIdentity
    )

    private companion object {
        const val PROTOCOL_VERSION = 1
        const val PROFILE_TYPE = "friend_profile"
    }
}

internal fun offlinePairingId(firstNonce: String, secondNonce: String): String {
    val canonical = listOf(firstNonce, secondNonce).sorted().joinToString("|")
    return MessageDigest.getInstance("SHA-256")
        .digest(canonical.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
