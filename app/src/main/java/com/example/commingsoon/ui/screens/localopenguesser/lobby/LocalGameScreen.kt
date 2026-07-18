package com.example.commingsoon.ui.screens.localopenguesser.lobby

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.commingsoon.R
import com.example.commingsoon.language.appString
import com.example.commingsoon.ui.screens.localopenguesser.OfflineGuessMap
import com.example.commingsoon.ui.screens.localopenguesser.connection.LocalGamePhase
import com.example.commingsoon.ui.screens.localopenguesser.connection.NearbyConnectionState
import com.example.commingsoon.ui.screens.localopenguesser.connection.NearbyRole
import org.maplibre.android.geometry.LatLng
import kotlin.math.roundToInt

@Composable
internal fun LocalGameScreen(
    state: NearbyConnectionState,
    onGuess: (Double, Double) -> Unit,
    onContinueAfterRound: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (state.game.phase) {
                LocalGamePhase.PREPARING,
                LocalGamePhase.WAITING_FOR_OTHER_PLAYER -> GameWaitingState(state)
                LocalGamePhase.TRANSFERRING_PHOTO -> PhotoTransferState(state)
                LocalGamePhase.PLAYING_ROUND -> RoundPhoto(state, onGuess)
                LocalGamePhase.ROUND_RESULT -> RoundResultCard(state, onContinueAfterRound)
                LocalGamePhase.SETUP,
                LocalGamePhase.FINISHED -> Unit
            }
            OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) {
                Text(appString(R.string.local_guesser_disconnect))
            }
        }
    }
}

@Composable
private fun GameWaitingState(state: NearbyConnectionState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircularProgressIndicator()
        Text(
            state.game.statusMessage?.resolve()
                ?: appString(R.string.local_guesser_preparing_game),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PhotoTransferState(state: NearbyConnectionState) {
    Text(
        appString(
            R.string.local_guesser_round_of,
            state.game.currentRound + 1,
            state.game.settings.roundCount
        ),
        style = MaterialTheme.typography.titleMedium
    )
    LinearProgressIndicator(
        progress = { state.game.transferProgress },
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        state.game.statusMessage?.resolve()
            ?: appString(R.string.local_guesser_transferring_photos)
    )
}

@Composable
private fun RoundPhoto(state: NearbyConnectionState, onGuess: (Double, Double) -> Unit) {
    Text(
        appString(
            R.string.local_guesser_round_of,
            state.game.currentRound + 1,
            state.game.settings.roundCount
        ),
        style = MaterialTheme.typography.titleMedium
    )
    Text(
        appString(R.string.local_guesser_seconds_short, state.game.secondsRemaining),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary
    )
    val bitmap = remember(state.game.receivedPhotoPath) {
        state.game.receivedPhotoPath?.let(BitmapFactory::decodeFile)?.asImageBitmap()
    }
    var roundView by remember(state.game.currentRound) { mutableStateOf(RoundView.PHOTO) }
    val selectedGuess = state.game.currentGuess?.let { LatLng(it.latitude, it.longitude) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (roundView == RoundView.PHOTO) {
            Button(onClick = { roundView = RoundView.PHOTO }, modifier = Modifier.weight(1f)) {
                Text(appString(R.string.local_guesser_photo))
            }
        } else {
            OutlinedButton(onClick = { roundView = RoundView.PHOTO }, modifier = Modifier.weight(1f)) {
                Text(appString(R.string.local_guesser_photo))
            }
        }
        if (roundView == RoundView.MAP) {
            Button(onClick = { roundView = RoundView.MAP }, modifier = Modifier.weight(1f)) {
                Text(appString(R.string.local_guesser_map))
            }
        } else {
            OutlinedButton(onClick = { roundView = RoundView.MAP }, modifier = Modifier.weight(1f)) {
                Text(appString(R.string.local_guesser_map))
            }
        }
    }

    when (roundView) {
        RoundView.PHOTO -> {
            if (bitmap == null) {
                Text(appString(R.string.local_guesser_photo_display_failed))
            } else {
                ZoomableRoundPhoto(bitmap = bitmap)
                Text(
                    appString(R.string.local_guesser_photo_zoom_help),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        RoundView.MAP -> {
            Box(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                OfflineGuessMap(
                    selectedLocation = selectedGuess,
                    onLocationSelected = { onGuess(it.latitude, it.longitude) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text(
                selectedGuess?.let { guess ->
                    appString(
                        R.string.local_guesser_guess_pinned,
                        "%.4f".format(guess.latitude),
                        "%.4f".format(guess.longitude)
                    )
                } ?: appString(R.string.local_guesser_tap_map_to_guess),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun RoundResultCard(state: NearbyConnectionState, onContinue: () -> Unit) {
    val result = state.game.currentRoundResult
    if (result == null) {
        GameWaitingState(state)
        return
    }
    val opponentName = state.connectedEndpoint?.name
        ?: appString(R.string.local_guesser_other_player)
    Text(
        appString(R.string.local_guesser_round_result, result.round + 1),
        style = MaterialTheme.typography.headlineSmall
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PlayerResultSummary(
            name = appString(R.string.local_guesser_you),
            distanceKm = result.localDistanceKm,
            points = result.localPoints,
            modifier = Modifier.weight(1f)
        )
        PlayerResultSummary(
            name = opponentName,
            distanceKm = result.opponentDistanceKm,
            points = result.opponentPoints,
            modifier = Modifier.weight(1f)
        )
    }
    OfflineGuessMap(
        selectedLocation = result.localGuess?.let { LatLng(it.latitude, it.longitude) },
        actualLocation = LatLng(result.actualLocation.latitude, result.actualLocation.longitude),
        isGuessingEnabled = false,
        onLocationSelected = {},
        modifier = Modifier.fillMaxWidth().height(420.dp)
    )
    Text(
        appString(R.string.local_guesser_result_map_help),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall
    )
    if (state.role == NearbyRole.HOST) {
        Button(
            onClick = onContinue,
            enabled = state.game.canContinueAfterRound,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (!state.game.canContinueAfterRound) {
                    appString(R.string.local_guesser_waiting_other_player)
                } else if (result.round + 1 >= state.game.settings.roundCount) {
                    appString(R.string.local_guesser_show_final_results)
                } else {
                    appString(R.string.local_guesser_start_round, result.round + 2)
                }
            )
        }
    } else {
        Text(
            if (result.round + 1 >= state.game.settings.roundCount) {
                appString(R.string.local_guesser_waiting_host_final_results)
            } else {
                appString(R.string.local_guesser_waiting_host_next_round)
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PlayerResultSummary(
    name: String,
    distanceKm: Double?,
    points: Int,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(name, style = MaterialTheme.typography.titleSmall)
            Text(formatDistance(distanceKm), style = MaterialTheme.typography.bodyMedium)
            Text(
                appString(R.string.local_guesser_points_value, points),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
internal fun formatDistance(distanceKm: Double?): String = when {
    distanceKm == null -> appString(R.string.local_guesser_no_guess)
    distanceKm < 1.0 -> appString(
        R.string.local_guesser_meters_away,
        (distanceKm * 1_000).roundToInt()
    )
    distanceKm < 100.0 -> appString(
        R.string.local_guesser_kilometers_away,
        "%.1f".format(distanceKm)
    )
    else -> appString(R.string.local_guesser_kilometers_away, distanceKm.roundToInt())
}

private enum class RoundView { PHOTO, MAP }

@Composable
private fun ZoomableRoundPhoto(bitmap: androidx.compose.ui.graphics.ImageBitmap) {
    var scale by remember(bitmap) { mutableFloatStateOf(1f) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    var viewportSize by remember(bitmap) { mutableStateOf(IntSize.Zero) }

    fun updateTransform(nextScale: Float, panChange: Offset = Offset.Zero) {
        val clampedScale = nextScale.coerceIn(1f, 5f)
        val maxX = viewportSize.width * (clampedScale - 1f) / 2f
        val maxY = viewportSize.height * (clampedScale - 1f) / 2f
        offset = if (clampedScale == 1f) {
            Offset.Zero
        } else {
            Offset(
                x = (offset.x + panChange.x).coerceIn(-maxX, maxX),
                y = (offset.y + panChange.y).coerceIn(-maxY, maxY)
            )
        }
        scale = clampedScale
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .clipToBounds()
            .onSizeChanged { viewportSize = it }
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = appString(R.string.local_guesser_round_photo_description),
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bitmap) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false).consume()
                        do {
                            val event = awaitPointerEvent()
                            updateTransform(
                                nextScale = scale * event.calculateZoom(),
                                panChange = event.calculatePan()
                            )
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            contentScale = ContentScale.Fit
        )
        Card(modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { updateTransform(scale / 1.5f) }) { Text("−") }
                Text("${(scale * 100).roundToInt()}%")
                TextButton(onClick = { updateTransform(scale * 1.5f) }) { Text("+") }
                if (scale > 1f) {
                    TextButton(onClick = { updateTransform(1f) }) {
                        Text(appString(R.string.local_guesser_reset_zoom))
                    }
                }
            }
        }
    }
}
