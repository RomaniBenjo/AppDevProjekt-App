package com.example.commingsoon.ui.screens.localopenguesser.lobby

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.commingsoon.ui.screens.localopenguesser.connection.LocalConnectionViewModel
import com.example.commingsoon.ui.screens.localopenguesser.connection.LocalGamePhase
import com.example.commingsoon.ui.screens.localopenguesser.connection.LocalGameSettings
import com.example.commingsoon.ui.screens.localopenguesser.connection.NearbyConnectionState
import com.example.commingsoon.ui.screens.localopenguesser.connection.NearbyEndpoint
import com.example.commingsoon.ui.screens.localopenguesser.connection.NearbyPhase
import com.example.commingsoon.ui.screens.localopenguesser.connection.NearbyRole
import android.graphics.BitmapFactory
import com.example.commingsoon.ui.screens.localopenguesser.connection.hasNearbyPermissions
import com.example.commingsoon.ui.screens.localopenguesser.connection.requiredNearbyPermissions

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
        Text("Connect players", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Nearby Connections uses Bluetooth and Wi-Fi directly between the two phones. " +
                "No game server or internet connection is used.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!playServicesAvailable) {
            ErrorCard(
                "Google Play services is unavailable or needs updating. " +
                    "Nearby Connections cannot run on this phone."
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
                label = { Text("Name shown to the other player") },
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
            Text("Back")
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
    onDisconnect: () -> Unit
) {
    when (state.phase) {
        NearbyPhase.IDLE -> RoleSelection(onHost, onJoin)
        NearbyPhase.ADVERTISING -> WaitingCard(
            title = "Hosting nearby game",
            detail = "Waiting for the second phone to find ${state.localName}…",
            onCancel = onCancel
        )
        NearbyPhase.DISCOVERING -> DiscoveryCard(state.discoveredEndpoints, onConnect, onCancel)
        NearbyPhase.REQUESTING_CONNECTION -> WaitingCard(
            title = "Requesting connection",
            detail = "Waiting for the other player…",
            onCancel = onCancel
        )
        NearbyPhase.AWAITING_CONFIRMATION,
        NearbyPhase.CONNECTING -> WaitingCard(
            title = "Connecting securely",
            detail = "Both players must confirm the same authentication code.",
            onCancel = onCancel
        )
        NearbyPhase.CONNECTED -> ConnectedCard(
            state = state,
            onStartGame = onStartGame,
            onDisconnect = onDisconnect
        )
        NearbyPhase.ERROR -> {
            ErrorCard(state.errorMessage ?: "An unknown nearby connection error occurred.")
            Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("Return to connection options")
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
            Text("Choose a role", style = MaterialTheme.typography.titleMedium)
            Text("One phone hosts the session and the other phone joins it.")
            Button(onClick = onHost, modifier = Modifier.fillMaxWidth()) {
                Text("Host game")
            }
            OutlinedButton(onClick = onJoin, modifier = Modifier.fillMaxWidth()) {
                Text("Join game")
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
                Text("Cancel")
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
                Text("Nearby games", style = MaterialTheme.typography.titleMedium)
            }
            if (endpoints.isEmpty()) {
                Text("Searching… Make sure the other phone selected Host game.")
            } else {
                endpoints.forEach { endpoint ->
                    Button(onClick = { onConnect(endpoint) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Connect to ${endpoint.name}")
                    }
                }
            }
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("Stop searching")
            }
        }
    }
}

@Composable
private fun ConnectedCard(
    state: NearbyConnectionState,
    onStartGame: (LocalGameSettings) -> Unit,
    onDisconnect: () -> Unit
) {
    var settings by remember { mutableStateOf(LocalGameSettings()) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Connected", style = MaterialTheme.typography.titleMedium)
            Text("Secure connection to ${state.connectedEndpoint?.name ?: "the other player"}")
            when (state.game.phase) {
                LocalGamePhase.SETUP -> {
                    state.game.statusMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                    if (state.role == NearbyRole.HOST) {
                        HostGameOptions(
                            settings = settings,
                            onSettingsChange = { settings = it },
                            onStart = { onStartGame(settings) }
                        )
                    } else {
                        Text("Waiting for the host to choose the game settings and start.")
                    }
                }
                LocalGamePhase.PREPARING,
                LocalGamePhase.WAITING_FOR_OTHER_PLAYER -> GameWaitingState(state)
                LocalGamePhase.TRANSFERRING_PHOTO -> PhotoTransferState(state)
                LocalGamePhase.PLAYING_ROUND -> RoundPhoto(state)
                LocalGamePhase.FINISHED -> {
                    Text("Game complete", style = MaterialTheme.typography.headlineSmall)
                    Text(state.game.statusMessage ?: "All rounds are finished.")
                }
            }
            OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) {
                Text("Disconnect")
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
    Text("Game options", style = MaterialTheme.typography.titleMedium)
    OptionStepper(
        label = "Round count",
        value = settings.roundCount,
        onDecrease = {
            onSettingsChange(settings.copy(roundCount = (settings.roundCount - 1).coerceAtLeast(1)))
        },
        onIncrease = {
            onSettingsChange(settings.copy(roundCount = (settings.roundCount + 1).coerceAtMost(20)))
        }
    )
    Text("Round length", style = MaterialTheme.typography.titleSmall)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(15, 30, 60).forEach { seconds ->
            if (settings.roundSeconds == seconds) {
                Button(
                    onClick = { onSettingsChange(settings.copy(roundSeconds = seconds)) },
                    modifier = Modifier.weight(1f)
                ) { Text("${seconds}s") }
            } else {
                OutlinedButton(
                    onClick = { onSettingsChange(settings.copy(roundSeconds = seconds)) },
                    modifier = Modifier.weight(1f)
                ) { Text("${seconds}s") }
            }
        }
    }
    Text(
        "Each device contributes one random geotagged photo per round. " +
            "No device will use more than two photos from the same country.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall
    )
    Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
        Text("Select photos and start")
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
        Text(state.game.statusMessage ?: "Preparing game…", textAlign = TextAlign.Center)
    }
}

@Composable
private fun PhotoTransferState(state: NearbyConnectionState) {
    Text(
        "Round ${state.game.currentRound + 1} of ${state.game.settings.roundCount}",
        style = MaterialTheme.typography.titleMedium
    )
    LinearProgressIndicator(
        progress = { state.game.transferProgress },
        modifier = Modifier.fillMaxWidth()
    )
    Text(state.game.statusMessage ?: "Transferring photos…")
}

@Composable
private fun RoundPhoto(state: NearbyConnectionState) {
    Text(
        "Round ${state.game.currentRound + 1} of ${state.game.settings.roundCount}",
        style = MaterialTheme.typography.titleMedium
    )
    Text(
        "${state.game.secondsRemaining}s",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary
    )
    val bitmap = remember(state.game.receivedPhotoPath) {
        state.game.receivedPhotoPath?.let(BitmapFactory::decodeFile)?.asImageBitmap()
    }
    if (bitmap == null) {
        Text("The other player's photo could not be displayed.")
    } else {
        Image(
            bitmap = bitmap,
            contentDescription = "Other player's round photo",
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
            contentScale = ContentScale.Fit
        )
    }
    Text("Map guessing will be added in the next step.")
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
        title = { Text("Confirm $playerName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Check that this code is identical on both phones:")
                Text(
                    digits,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text("Accept only if both players see the same code.")
            }
        },
        confirmButton = { TextButton(onClick = onAccept) { Text("Codes match") } },
        dismissButton = { TextButton(onClick = onReject) { Text("Reject") } }
    )
}

@Composable
private fun PermissionCard(onRequestPermissions: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Nearby-device access required", style = MaterialTheme.typography.titleMedium)
            Text(
                "Android needs permission to discover and connect through Bluetooth and nearby Wi-Fi."
            )
            Button(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth()) {
                Text("Allow nearby devices")
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
