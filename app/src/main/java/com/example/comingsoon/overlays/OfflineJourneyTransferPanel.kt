package com.example.comingsoon.overlays

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.comingsoon.R
import com.example.comingsoon.friends.OfflinePairingPhase
import com.example.comingsoon.language.appString
import com.example.comingsoon.viewmodels.FriendViewModel

@Composable
fun OfflineJourneyTransferPanel(
    viewModel: FriendViewModel,
    onDone: () -> Unit
) {
    val state by viewModel.offlinePairingState.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = appString(
                R.string.offline_journey_share_title,
                state.targetFriendName.orEmpty()
            ),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = appString(R.string.offline_journey_recipient_instruction),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))

        when (state.phase) {
            OfflinePairingPhase.DISCOVERING -> {
                Text(appString(R.string.offline_friend_searching))
                Spacer(Modifier.height(12.dp))
                if (state.discoveredEndpoints.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                state.discoveredEndpoints.forEach { endpoint ->
                    Button(
                        onClick = { viewModel.connectOfflineFriend(endpoint) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(appString(R.string.offline_friend_connect_to, endpoint.name))
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            OfflinePairingPhase.AWAITING_CONFIRMATION -> {
                val pending = state.pendingConnection
                Text(
                    text = appString(
                        R.string.offline_friend_confirm_person,
                        pending?.endpoint?.name.orEmpty()
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(appString(R.string.offline_friend_compare_code))
                Text(
                    text = pending?.authenticationDigits.orEmpty(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = viewModel::acceptOfflineFriend,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(appString(R.string.offline_friend_code_matches))
                }
                OutlinedButton(
                    onClick = viewModel::rejectOfflineFriend,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(appString(R.string.reject))
                }
            }

            OfflinePairingPhase.REQUESTING,
            OfflinePairingPhase.CONNECTING,
            OfflinePairingPhase.EXCHANGING_PROFILE,
            OfflinePairingPhase.TRANSFERRING_JOURNEY -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = appString(R.string.offline_journey_transferring),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            OfflinePairingPhase.JOURNEY_SHARED -> {
                Text(
                    text = appString(
                        R.string.offline_journey_shared,
                        state.journeyTitle.orEmpty(),
                        state.pairedFriendName.orEmpty()
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(14.dp))
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text(appString(R.string.done))
                }
            }

            OfflinePairingPhase.ERROR -> {
                Text(
                    text = state.errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error
                )
            }

            else -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        if (
            state.phase != OfflinePairingPhase.JOURNEY_SHARED &&
            state.phase != OfflinePairingPhase.JOURNEY_RECEIVED
        ) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    viewModel.stopOfflinePairing()
                    onDone()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(appString(R.string.cancel))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
