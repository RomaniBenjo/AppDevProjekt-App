package com.example.commingsoon.ui.screens.localopenguesser.lobby

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.commingsoon.R
import com.example.commingsoon.language.appString
import com.example.commingsoon.ui.screens.localopenguesser.connection.LocalConnectionViewModel
import com.example.commingsoon.ui.screens.localopenguesser.connection.LocalGamePhase
import com.example.commingsoon.ui.screens.localopenguesser.connection.LocalGuesserMessage
import com.example.commingsoon.ui.screens.localopenguesser.connection.NearbyPhase
import com.example.commingsoon.ui.screens.localopenguesser.connection.hasNearbyPermissions
import com.example.commingsoon.ui.screens.localopenguesser.connection.requiredNearbyPermissions

@Composable
internal fun LocalGameLobbyScreen(
    navController: NavHostController,
    profileName: String,
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

    LaunchedEffect(profileName) {
        if (profileName.isNotBlank()) {
            connectionViewModel.setLocalName(profileName)
        }
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

        when {
            !playServicesAvailable -> LocalGameErrorCard(
                appString(R.string.local_guesser_play_services_unavailable)
            )
            !permissionsGranted -> PermissionCard {
                permissionLauncher.launch(requiredNearbyPermissions())
            }
            state.phase != NearbyPhase.CONNECTED -> LocalGamePregameScreen(
                state = state,
                localName = localName,
                onNameChange = {
                    localName = it.take(32)
                    connectionViewModel.setLocalName(localName)
                },
                onHost = connectionViewModel::host,
                onJoin = connectionViewModel::join,
                onConnect = connectionViewModel::connect,
                onCancel = connectionViewModel::stopSearching
            )
            state.game.phase == LocalGamePhase.SETUP -> LocalGameConnectedLobbyScreen(
                state = state,
                onStartGame = connectionViewModel::startGame,
                onDisconnect = connectionViewModel::disconnect
            )
            state.game.phase == LocalGamePhase.FINISHED -> LocalGameWinScreen(
                state = state,
                onDisconnect = connectionViewModel::disconnect
            )
            else -> LocalGameScreen(
                state = state,
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
internal fun LocalGuesserMessage.resolve(): String =
    appString(resourceId, *args.toTypedArray())
