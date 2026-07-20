package com.example.comingsoon.ui.screens.localopenguesser.lobby

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.comingsoon.R
import com.example.comingsoon.language.appString
import com.example.comingsoon.ui.screens.localopenguesser.connection.NearbyConnectionState
import com.example.comingsoon.ui.screens.localopenguesser.connection.RoundResult

@Composable
internal fun LocalGameWinScreen(
    state: NearbyConnectionState,
    onDisconnect: () -> Unit
) {
    val results = state.game.roundResults.sortedBy(RoundResult::round)
    val localTotal = results.sumOf(RoundResult::localPoints)
    val opponentTotal = results.sumOf(RoundResult::opponentPoints)
    val opponentName = state.connectedEndpoint?.name
        ?: appString(R.string.local_guesser_other_player)
    val resultHeadline = when {
        localTotal > opponentTotal -> appString(R.string.local_guesser_you_win)
        localTotal < opponentTotal -> appString(R.string.local_guesser_player_wins, opponentName)
        else -> appString(R.string.local_guesser_tie)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    appString(R.string.local_guesser_game_complete),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    resultHeadline,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FinalScoreCard(
                name = appString(R.string.local_guesser_you),
                points = localTotal,
                isWinner = localTotal >= opponentTotal,
                modifier = Modifier.weight(1f)
            )
            FinalScoreCard(
                name = opponentName,
                points = opponentTotal,
                isWinner = opponentTotal >= localTotal,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            appString(R.string.local_guesser_points_by_round),
            style = MaterialTheme.typography.titleLarge
        )
        results.forEachIndexed { index, result ->
            val localRunningTotal = results.take(index + 1).sumOf(RoundResult::localPoints)
            val opponentRunningTotal = results.take(index + 1).sumOf(RoundResult::opponentPoints)
            RoundScoreCard(
                result = result,
                localName = appString(R.string.local_guesser_you),
                opponentName = opponentName,
                localRunningTotal = localRunningTotal,
                opponentRunningTotal = opponentRunningTotal
            )
        }

        OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) {
            Text(appString(R.string.local_guesser_disconnect))
        }
    }
}

@Composable
private fun FinalScoreCard(
    name: String,
    points: Int,
    isWinner: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isWinner) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isWinner) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isWinner) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            Text(
                name,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                points.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                appString(R.string.local_guesser_points),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun RoundScoreCard(
    result: RoundResult,
    localName: String,
    opponentName: String,
    localRunningTotal: Int,
    opponentRunningTotal: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                appString(R.string.local_guesser_round_number, result.round + 1),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            RoundPlayerScore(
                name = localName,
                roundPoints = result.localPoints,
                runningTotal = localRunningTotal,
                distance = formatDistance(result.localDistanceKm),
                wonRound = result.localPoints > result.opponentPoints
            )
            RoundPlayerScore(
                name = opponentName,
                roundPoints = result.opponentPoints,
                runningTotal = opponentRunningTotal,
                distance = formatDistance(result.opponentDistanceKm),
                wonRound = result.opponentPoints > result.localPoints
            )
        }
    }
}

@Composable
private fun RoundPlayerScore(
    name: String,
    roundPoints: Int,
    runningTotal: Int,
    distance: String,
    wonRound: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                if (wonRound) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                distance,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "+$roundPoints",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                appString(R.string.local_guesser_points_value, runningTotal),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
