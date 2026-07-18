package com.example.commingsoon.ui.screens.localopenguesser.connection

import androidx.annotation.StringRes

internal data class LocalGuesserMessage(
    @param:StringRes val resourceId: Int,
    val args: List<Any> = emptyList()
)

internal enum class NearbyPhase {
    IDLE,
    ADVERTISING,
    DISCOVERING,
    REQUESTING_CONNECTION,
    AWAITING_CONFIRMATION,
    CONNECTING,
    CONNECTED,
    ERROR
}

internal enum class NearbyRole { HOST, JOINER }

internal enum class LocalGamePhase {
    SETUP,
    PREPARING,
    WAITING_FOR_OTHER_PLAYER,
    TRANSFERRING_PHOTO,
    PLAYING_ROUND,
    ROUND_RESULT,
    FINISHED
}

internal enum class HomePhotoExclusionMode {
    NONE,
    MOST_PHOTOGRAPHED_COUNTRY,
    LARGEST_HOME_CLUSTER
}

internal data class LocalGameSettings(
    val roundCount: Int = 5,
    val roundSeconds: Int = 30,
    val homePhotoExclusionMode: HomePhotoExclusionMode = HomePhotoExclusionMode.NONE
)

internal data class GuessLocation(
    val latitude: Double,
    val longitude: Double
)

internal data class RoundResult(
    val round: Int,
    val localGuess: GuessLocation?,
    val actualLocation: GuessLocation,
    val localDistanceKm: Double?,
    val localPoints: Int,
    val opponentDistanceKm: Double?,
    val opponentPoints: Int
)

internal data class LocalGameState(
    val phase: LocalGamePhase = LocalGamePhase.SETUP,
    val settings: LocalGameSettings = LocalGameSettings(),
    val currentRound: Int = -1,
    val receivedPhotoPath: String? = null,
    val transferProgress: Float = 0f,
    val secondsRemaining: Int = 0,
    val currentGuess: GuessLocation? = null,
    val currentRoundResult: RoundResult? = null,
    val roundResults: List<RoundResult> = emptyList(),
    val canContinueAfterRound: Boolean = false,
    val statusMessage: LocalGuesserMessage? = null
)

internal data class NearbyEndpoint(
    val id: String,
    val name: String
)

internal data class PendingConnection(
    val endpoint: NearbyEndpoint,
    val authenticationDigits: String
)

internal data class NearbyConnectionState(
    val phase: NearbyPhase = NearbyPhase.IDLE,
    val localName: String = "",
    val role: NearbyRole? = null,
    val discoveredEndpoints: List<NearbyEndpoint> = emptyList(),
    val pendingConnection: PendingConnection? = null,
    val connectedEndpoint: NearbyEndpoint? = null,
    val receivedTestMessage: String? = null,
    val game: LocalGameState = LocalGameState(),
    val errorMessage: LocalGuesserMessage? = null
)
