package com.example.comingsoon.overlays

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.comingsoon.language.appString
import com.example.comingsoon.language.appDateString
import com.example.comingsoon.viewmodels.Friend
import com.example.comingsoon.viewmodels.FriendViewModel
import com.example.comingsoon.viewmodels.Journey
import com.example.comingsoon.R
import com.example.comingsoon.friends.OfflinePairingMode
import com.example.comingsoon.friends.OfflinePairingPhase
import com.example.comingsoon.ui.screens.localopenguesser.connection.hasNearbyPermissions
import com.example.comingsoon.ui.screens.localopenguesser.connection.requiredNearbyPermissions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareWithFriendOverlay (
    friend: Friend,
    journeys: List<Journey>,
    friendViewModel: FriendViewModel,
    isNetworkAvailable: Boolean,
    shareTypesByJourneyId: Map<Int, String> = emptyMap(),
    operationKey: String? = null,
    errorMessage: String? = null,
    feedbackMessage: String? = null,
    onDismiss: () -> Unit,
    onShare: (Journey) -> Unit,
    onUnshare: (Journey) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val offlineState by friendViewModel.offlinePairingState.collectAsState()
    var pendingOfflineJourney by remember { mutableStateOf<Journey?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val journey = pendingOfflineJourney
        pendingOfflineJourney = null
        if (journey != null && result.values.all { it }) {
            friendViewModel.shareJourneyOffline(journey, friend)
        }
    }
    val shareJourney: (Journey) -> Unit = { journey ->
        val useNearby = friend.addedNearby && (!isNetworkAvailable || friend.id <= 0)
        if (!useNearby) {
            onShare(journey)
        } else if (hasNearbyPermissions(context)) {
            friendViewModel.shareJourneyOffline(journey, friend)
        } else {
            pendingOfflineJourney = journey
            permissionLauncher.launch(requiredNearbyPermissions())
        }
    }
    val offlineTransferActive =
        offlineState.mode == OfflinePairingMode.JOURNEY_SHARING &&
            offlineState.phase != OfflinePairingPhase.IDLE

    ModalBottomSheet(
        onDismissRequest = {
            if (offlineTransferActive) friendViewModel.stopOfflinePairing()
            onDismiss()
        },
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // Handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 20.dp)
                    .height(5.dp)
                    .fillMaxWidth(.18f)
                    .background(
                        Color.LightGray,
                        RoundedCornerShape(50)
                    )
            )

            if (offlineTransferActive) {
                OfflineJourneyTransferPanel(
                    viewModel = friendViewModel,
                    onDone = {
                        friendViewModel.stopOfflinePairing()
                        onDismiss()
                    }
                )
                return@Column
            }

            Text(
                text = friend.name,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = appString(R.string.share_journey_with_friend),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            feedbackMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(24.dp))

            JourneyShareList(
                journeys = journeys,
                friendId = friend.id,
                shareTypesByJourneyId = shareTypesByJourneyId,
                operationKey = operationKey,
                onShare = shareJourney,
                onUnshare = onUnshare
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun JourneyShareList(
    journeys: List<Journey>,
    friendId: Int,
    shareTypesByJourneyId: Map<Int, String>,
    operationKey: String?,
    onShare: (Journey) -> Unit,
    onUnshare: (Journey) -> Unit
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 450.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(journeys) { journey ->
            JourneyShareItem(
                journey = journey,
                shareType = shareTypesByJourneyId[journey.id],
                isLoading = operationKey == "${journey.id}:$friendId",
                onShare = {
                    onShare(journey)
                },
                onUnshare = { onUnshare(journey) }
            )
            HorizontalDivider(
                color = Color.LightGray.copy(alpha = .3f),
                modifier = Modifier.padding(start = 50.dp)
            )
        }

        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun JourneyShareItem(
    journey: Journey,
    shareType: String?,
    isLoading: Boolean,
    onShare: () -> Unit,
    onUnshare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = journey.title,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = "${appDateString(journey.startDate)} – ${appDateString(journey.endDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        FilledTonalButton(
            onClick = if (shareType == "manual") onUnshare else onShare,
            enabled = !isLoading && shareType != "automatic" && shareType != "offline",
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.background
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = if (shareType == "manual") {
                        Icons.Outlined.LinkOff
                    } else {
                        Icons.Outlined.Share
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    when (shareType) {
                        "manual" -> appString(R.string.unshare)
                        "automatic" -> appString(R.string.shared_automatically)
                        "offline" -> appString(R.string.offline_shared)
                        else -> appString(R.string.share)
                    }
                )
            }
        }
    }
}
