package com.example.comingsoon.ui.screens.localopenguesser.lobby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.comingsoon.R
import com.example.comingsoon.language.appString
import com.example.comingsoon.ui.screens.localopenguesser.connection.HomePhotoExclusionMode
import com.example.comingsoon.ui.screens.localopenguesser.connection.LocalGameSettings
import com.example.comingsoon.ui.screens.localopenguesser.connection.NearbyConnectionState
import com.example.comingsoon.ui.screens.localopenguesser.connection.NearbyRole

@Composable
internal fun LocalGameConnectedLobbyScreen(
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
            Text(appString(R.string.local_guesser_connected), style = MaterialTheme.typography.titleMedium)
            Text(
                appString(
                    R.string.local_guesser_secure_connection_to,
                    state.connectedEndpoint?.name
                        ?: appString(R.string.local_guesser_other_player_lowercase)
                )
            )
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
    Text(appString(R.string.local_guesser_home_photo_filter), style = MaterialTheme.typography.titleSmall)
    HomePhotoFilterOption(
        selected = settings.homePhotoExclusionMode == HomePhotoExclusionMode.NONE,
        label = appString(R.string.local_guesser_home_photo_filter_none),
        onClick = {
            onSettingsChange(settings.copy(homePhotoExclusionMode = HomePhotoExclusionMode.NONE))
        }
    )
    HomePhotoFilterOption(
        selected = settings.homePhotoExclusionMode == HomePhotoExclusionMode.MOST_PHOTOGRAPHED_COUNTRY,
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
                settings.copy(homePhotoExclusionMode = HomePhotoExclusionMode.LARGEST_HOME_CLUSTER)
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
private fun HomePhotoFilterOption(selected: Boolean, label: String, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label) }
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onDecrease) { Text("−") }
            Text(value.toString(), style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = onIncrease) { Text("+") }
        }
    }
}
