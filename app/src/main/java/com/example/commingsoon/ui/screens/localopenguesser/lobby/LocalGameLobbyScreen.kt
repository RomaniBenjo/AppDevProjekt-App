package com.example.commingsoon.ui.screens.localopenguesser.lobby

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.commingsoon.ui.screens.localopenguesser.connection.LocalConnectionViewModel
import com.example.commingsoon.ui.screens.localopenguesser.OfflineGuessMap
import com.example.commingsoon.ui.screens.localopenguesser.connection.HomePhotoExclusionMode
import com.example.commingsoon.ui.screens.localopenguesser.connection.LocalGamePhase
import com.example.commingsoon.ui.screens.localopenguesser.connection.LocalGameSettings
import com.example.commingsoon.ui.screens.localopenguesser.connection.LocalGuesserMessage
import com.example.commingsoon.ui.screens.localopenguesser.connection.RoundResult
import com.example.commingsoon.ui.screens.localopenguesser.connection.NearbyConnectionState
import com.example.commingsoon.ui.screens.localopenguesser.connection.NearbyEndpoint
import com.example.commingsoon.ui.screens.localopenguesser.connection.NearbyPhase
import com.example.commingsoon.ui.screens.localopenguesser.connection.NearbyRole
import android.graphics.BitmapFactory
import androidx.appcompat.R as AppCompatR
import com.example.commingsoon.language.appString
import com.example.commingsoon.ui.screens.localopenguesser.connection.hasNearbyPermissions
import com.example.commingsoon.ui.screens.localopenguesser.connection.requiredNearbyPermissions
import org.maplibre.android.geometry.LatLng
import kotlin.math.roundToInt
import com.example.commingsoon.R

@Composable
internal fun LocalGameLobbyScreen(
    navController: NavHostController,
    connectionViewModel: LocalConnectionViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by connectionViewModel.state.collectAsState()
    var permissionsGranted by remember { mutableStateOf(hasNearbyPermissions(context)) }
    var localName by remember(state.localName) { mutableStateOf(state.localName) }
    val playServicesAvailable = remember { connectionViewModel.hasGooglePlayServices() }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionsGranted = hasNearbyPermissions(context)
    }

    state.pendingConnection?.let { pending ->
        AuthenticationDialog(
            playerName = pending.endpoint.name,
            digits = pending.authenticationDigits,
            onAccept = connectionViewModel::accept,
            onReject = connectionViewModel::reject
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(appString(R.string.guesser_connect), style = MaterialTheme.typography.headlineSmall)
        Text(
            appString(R.string.local_guesser_nearby_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!playServicesAvailable) {
            ErrorCard(
                appString(R.string.local_guesser_play_services_unavailable)
            )
        } else if (!permissionsGranted) {
            PermissionCard {
                permissionLauncher.launch(requiredNearbyPermissions())
            }
        } else {
            OutlinedTextField(
                value = localName,
                onValueChange = {
                    localName = it.take(32)
                    connectionViewModel.setLocalName(localName)
                },
                enabled = state.phase == NearbyPhase.IDLE || state.phase == NearbyPhase.ERROR,
                label = { Text(appString(R.string.local_guesser_player_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            LobbyContent(
                state = state,
                onHost = connectionViewModel::host,
                onJoin = connectionViewModel::join,
                onConnect = connectionViewModel::connect,
                onCancel = connectionViewModel::stopSearching,
                onStartGame = connectionViewModel::startGame,
                onGuess = connectionViewModel::setGuess,
                onContinueAfterRound = connectionViewModel::continueAfterRound,
                onDisconnect = connectionViewModel::disconnect
            )
        }

        Spacer(Modifier.height(4.dp))
        OutlinedButton(
            onClick = {
                connectionViewModel.disconnect()
                navController.popBackStack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(appString(R.string.back))
        }
    }
}

@Composable
private fun LobbyContent(
    state: NearbyConnectionState,
    onHost: () -> Unit,
    onJoin: () -> Unit,
    onConnect: (NearbyEndpoint) -> Unit,
    onCancel: () -> Unit,
    onStartGame: (LocalGameSettings) -> Unit,
    onGuess: (Double, Double) -> Unit,
    onContinueAfterRound: () -> Unit,
    onDisconnect: () -> Unit
) {
    when (state.phase) {
        NearbyPhase.IDLE -> RoleSelection(onHost, onJoin)
        NearbyPhase.ADVERTISING -> WaitingCard(
            title = appString(R.string.local_guesser_hosting_game),
            detail = appString(R.string.local_guesser_waiting_to_find_host, state.localName),
            onCancel = onCancel
        )
        NearbyPhase.DISCOVERING -> DiscoveryCard(state.discoveredEndpoints, onConnect, onCancel)
        NearbyPhase.REQUESTING_CONNECTION -> WaitingCard(
            title = appString(R.string.local_guesser_requesting_connection),
            detail = appString(R.string.local_guesser_waiting_other_player),
            onCancel = onCancel
        )
        NearbyPhase.AWAITING_CONFIRMATION,
        NearbyPhase.CONNECTING -> WaitingCard(
            title = appString(R.string.local_guesser_connecting_securely),
            detail = appString(R.string.local_guesser_confirm_auth_code),
            onCancel = onCancel
        )
        NearbyPhase.CONNECTED -> ConnectedCard(
            state = state,
            onStartGame = onStartGame,
            onGuess = onGuess,
            onContinueAfterRound = onContinueAfterRound,
            onDisconnect = onDisconnect
        )
        NearbyPhase.ERROR -> {
            ErrorCard(
                state.errorMessage?.resolve()
                    ?: appString(R.string.local_guesser_unknown_connection_error)
            )
            Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text(appString(R.string.local_guesser_return_connection_options))
            }
        }
    }
}

@Composable
private fun RoleSelection(onHost: () -> Unit, onJoin: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(appString(R.string.local_guesser_choose_role), style = MaterialTheme.typography.titleMedium)
            Text(appString(R.string.local_guesser_role_description))
            Button(onClick = onHost, modifier = Modifier.fillMaxWidth()) {
                Text(appString(R.string.local_guesser_host_game))
            }
            OutlinedButton(onClick = onJoin, modifier = Modifier.fillMaxWidth()) {
                Text(appString(R.string.local_guesser_join_game))
            }
        }
    }
}

@Composable
private fun WaitingCard(title: String, detail: String, onCancel: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator()
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, textAlign = TextAlign.Center)
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text(appString(R.string.cancel))
            }
        }
    }
}

@Composable
private fun DiscoveryCard(
    endpoints: List<NearbyEndpoint>,
    onConnect: (NearbyEndpoint) -> Unit,
    onCancel: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator()
                Text(appString(R.string.local_guesser_nearby_games), style = MaterialTheme.typography.titleMedium)
            }
            if (endpoints.isEmpty()) {
                Text(appString(R.string.local_guesser_searching_games))
            } else {
                endpoints.forEach { endpoint ->
                    Button(onClick = { onConnect(endpoint) }, modifier = Modifier.fillMaxWidth()) {
                        Text(appString(R.string.local_guesser_connect_to_player, endpoint.name))
                    }
                }
            }
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text(appString(R.string.local_guesser_stop_searching))
            }
        }
    }
}

@Composable
private fun ConnectedCard(
    state: NearbyConnectionState,
    onStartGame: (LocalGameSettings) -> Unit,
    onGuess: (Double, Double) -> Unit,
    onContinueAfterRound: () -> Unit,
    onDisconnect: () -> Unit
) {
    var settings by remember { mutableStateOf(LocalGameSettings()) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(appString(R.string.local_guesser_connected), style = MaterialTheme.typography.titleMedium)
            Text(
                appString(
                    R.string.local_guesser_secure_connection_to,
                    state.connectedEndpoint?.name
                        ?: appString(R.string.local_guesser_other_player_lowercase)
                )
            )
            when (state.game.phase) {
                LocalGamePhase.SETUP -> {
                    state.game.statusMessage?.let {
                        Text(it.resolve(), color = MaterialTheme.colorScheme.error)
                    }
                    if (state.role == NearbyRole.HOST) {
                        HostGameOptions(
                            settings = settings,
                            onSettingsChange = { settings = it },
                            onStart = { onStartGame(settings) }
                        )
                    } else {
                        Text(appString(R.string.local_guesser_waiting_host_settings))
                    }
                }
                LocalGamePhase.PREPARING,
                LocalGamePhase.WAITING_FOR_OTHER_PLAYER -> GameWaitingState(state)
                LocalGamePhase.TRANSFERRING_PHOTO -> PhotoTransferState(state)
                LocalGamePhase.PLAYING_ROUND -> RoundPhoto(state, onGuess)
                LocalGamePhase.ROUND_RESULT -> RoundResultCard(
                    state = state,
                    onContinue = onContinueAfterRound
                )
                LocalGamePhase.FINISHED -> {
                    FinalScoreboard(state)
                }
            }
            OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) {
                Text(appString(R.string.local_guesser_disconnect))
            }
        }
    }
}

@Composable
private fun HostGameOptions(
    settings: LocalGameSettings,
    onSettingsChange: (LocalGameSettings) -> Unit,
    onStart: () -> Unit
) {
    Text(appString(R.string.local_guesser_game_options), style = MaterialTheme.typography.titleMedium)
    OptionStepper(
        label = appString(R.string.local_guesser_round_count),
        value = settings.roundCount,
        onDecrease = {
            onSettingsChange(settings.copy(roundCount = (settings.roundCount - 1).coerceAtLeast(1)))
        },
        onIncrease = {
            onSettingsChange(settings.copy(roundCount = (settings.roundCount + 1).coerceAtMost(20)))
        }
    )
    Text(appString(R.string.local_guesser_round_length), style = MaterialTheme.typography.titleSmall)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(15, 30, 60).forEach { seconds ->
            if (settings.roundSeconds == seconds) {
                Button(
                    onClick = { onSettingsChange(settings.copy(roundSeconds = seconds)) },
                    modifier = Modifier.weight(1f)
                ) { Text(appString(R.string.local_guesser_seconds_short, seconds)) }
            } else {
                OutlinedButton(
                    onClick = { onSettingsChange(settings.copy(roundSeconds = seconds)) },
                    modifier = Modifier.weight(1f)
                ) { Text(appString(R.string.local_guesser_seconds_short, seconds)) }
            }
        }
    }
    Text(
        appString(R.string.local_guesser_home_photo_filter),
        style = MaterialTheme.typography.titleSmall
    )
    HomePhotoFilterOption(
        selected = settings.homePhotoExclusionMode == HomePhotoExclusionMode.NONE,
        label = appString(R.string.local_guesser_home_photo_filter_none),
        onClick = {
            onSettingsChange(
                settings.copy(homePhotoExclusionMode = HomePhotoExclusionMode.NONE)
            )
        }
    )
    HomePhotoFilterOption(
        selected = settings.homePhotoExclusionMode ==
            HomePhotoExclusionMode.MOST_PHOTOGRAPHED_COUNTRY,
        label = appString(R.string.local_guesser_home_photo_filter_country),
        onClick = {
            onSettingsChange(
                settings.copy(
                    homePhotoExclusionMode = HomePhotoExclusionMode.MOST_PHOTOGRAPHED_COUNTRY
                )
            )
        }
    )
    HomePhotoFilterOption(
        selected = settings.homePhotoExclusionMode == HomePhotoExclusionMode.LARGEST_HOME_CLUSTER,
        label = appString(R.string.local_guesser_home_photo_filter_cluster),
        onClick = {
            onSettingsChange(
                settings.copy(
                    homePhotoExclusionMode = HomePhotoExclusionMode.LARGEST_HOME_CLUSTER
                )
            )
        }
    )
    Text(
        appString(R.string.local_guesser_home_photo_filter_description),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        appString(R.string.local_guesser_game_options_description),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall
    )
    Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
        Text(appString(R.string.local_guesser_select_photos_start))
    }
}

@Composable
private fun HomePhotoFilterOption(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    if (selected) {
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(label)
        }
    }
}

@Composable
private fun OptionStepper(
    label: String,
    value: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onDecrease) { Text("−") }
            Text(value.toString(), style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = onIncrease) { Text("+") }
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
private fun RoundPhoto(
    state: NearbyConnectionState,
    onGuess: (Double, Double) -> Unit
) {
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
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
private fun RoundResultCard(
    state: NearbyConnectionState,
    onContinue: () -> Unit
) {
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
        actualLocation = LatLng(
            result.actualLocation.latitude,
            result.actualLocation.longitude
        ),
        isGuessingEnabled = false,
        onLocationSelected = {},
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
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
private fun FinalScoreboard(state: NearbyConnectionState) {
    val results = state.game.roundResults.sortedBy(RoundResult::round)
    val localTotal = results.sumOf(RoundResult::localPoints)
    val opponentTotal = results.sumOf(RoundResult::opponentPoints)
    val opponentName = state.connectedEndpoint?.name
        ?: appString(R.string.local_guesser_other_player)
    Text(appString(R.string.local_guesser_game_complete), style = MaterialTheme.typography.headlineSmall)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TotalScore(appString(R.string.local_guesser_you), localTotal, Modifier.weight(1f))
        TotalScore(opponentName, opponentTotal, Modifier.weight(1f))
    }
    Text(
        when {
            localTotal > opponentTotal -> appString(R.string.local_guesser_you_win)
            localTotal < opponentTotal -> appString(
                R.string.local_guesser_player_wins,
                opponentName
            )
            else -> appString(R.string.local_guesser_tie)
        },
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary
    )
    Text(appString(R.string.local_guesser_points_by_round), style = MaterialTheme.typography.titleMedium)
    results.forEachIndexed { index, result ->
        val localRunningTotal = results.take(index + 1).sumOf(RoundResult::localPoints)
        val opponentRunningTotal = results.take(index + 1).sumOf(RoundResult::opponentPoints)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    appString(R.string.local_guesser_round_number, result.round + 1),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    appString(
                        R.string.local_guesser_round_score_line,
                        appString(R.string.local_guesser_you),
                        result.localPoints,
                        localRunningTotal,
                        formatDistance(result.localDistanceKm)
                    )
                )
                Text(
                    appString(
                        R.string.local_guesser_round_score_line,
                        opponentName,
                        result.opponentPoints,
                        opponentRunningTotal,
                        formatDistance(result.opponentDistanceKm)
                    )
                )
            }
        }
    }
}

@Composable
private fun TotalScore(name: String, points: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(name, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            Text(points.toString(), style = MaterialTheme.typography.headlineMedium)
            Text(appString(R.string.local_guesser_points))
        }
    }
}

@Composable
private fun formatDistance(distanceKm: Double?): String = when {
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
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(8.dp)
        ) {
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

@Composable
private fun AuthenticationDialog(
    playerName: String,
    digits: String,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onReject,
        title = { Text(appString(R.string.local_guesser_confirm_player, playerName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(appString(R.string.local_guesser_check_auth_code))
                Text(
                    digits,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(appString(R.string.local_guesser_accept_matching_code))
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text(appString(R.string.local_guesser_codes_match))
            }
        },
        dismissButton = {
            TextButton(onClick = onReject) {
                Text(appString(R.string.local_guesser_reject))
            }
        }
    )
}

@Composable
private fun PermissionCard(onRequestPermissions: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                appString(R.string.local_guesser_nearby_access_required),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                appString(R.string.local_guesser_nearby_permission_description)
            )
            Button(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth()) {
                Text(appString(R.string.local_guesser_allow_nearby_devices))
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            message,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun LocalGuesserMessage.resolve(): String =
    appString(resourceId, *args.toTypedArray())
