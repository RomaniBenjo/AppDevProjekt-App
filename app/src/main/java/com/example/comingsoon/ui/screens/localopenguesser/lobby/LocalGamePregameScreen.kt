package com.example.comingsoon.ui.screens.localopenguesser.lobby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.comingsoon.R
import com.example.comingsoon.language.appString
import com.example.comingsoon.ui.screens.localopenguesser.connection.NearbyConnectionState
import com.example.comingsoon.ui.screens.localopenguesser.connection.NearbyEndpoint
import com.example.comingsoon.ui.screens.localopenguesser.connection.NearbyPhase

@Composable
internal fun LocalGamePregameScreen(
    state: NearbyConnectionState,
    localName: String,
    onNameChange: (String) -> Unit,
    onHost: () -> Unit,
    onJoin: () -> Unit,
    onConnect: (NearbyEndpoint) -> Unit,
    onCancel: () -> Unit
) {
    OutlinedTextField(
        value = localName,
        onValueChange = onNameChange,
        enabled = state.phase == NearbyPhase.IDLE || state.phase == NearbyPhase.ERROR,
        label = { Text(appString(R.string.local_guesser_player_name_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

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
        NearbyPhase.ERROR -> {
            LocalGameErrorCard(
                state.errorMessage?.resolve()
                    ?: appString(R.string.local_guesser_unknown_connection_error)
            )
            Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text(appString(R.string.local_guesser_return_connection_options))
            }
        }
        NearbyPhase.CONNECTED -> Unit
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
internal fun AuthenticationDialog(
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
            TextButton(onClick = onAccept) { Text(appString(R.string.local_guesser_codes_match)) }
        },
        dismissButton = {
            TextButton(onClick = onReject) { Text(appString(R.string.local_guesser_reject)) }
        }
    )
}

@Composable
internal fun PermissionCard(onRequestPermissions: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                appString(R.string.local_guesser_nearby_access_required),
                style = MaterialTheme.typography.titleMedium
            )
            Text(appString(R.string.local_guesser_nearby_permission_description))
            Button(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth()) {
                Text(appString(R.string.local_guesser_allow_nearby_devices))
            }
        }
    }
}

@Composable
internal fun LocalGameErrorCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            message,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.error
        )
    }
}
