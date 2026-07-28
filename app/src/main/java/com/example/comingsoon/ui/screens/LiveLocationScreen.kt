package com.example.comingsoon.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.comingsoon.language.appString
import com.example.comingsoon.R
import com.example.comingsoon.location.rememberLiveLocationPermissionFlow
import com.example.comingsoon.ui.screens.livelocation.LiveLocationMap
import com.example.comingsoon.ui.screens.livelocation.LiveMapEntry
import com.example.comingsoon.viewmodels.LiveLocationViewModel
import org.maplibre.android.geometry.LatLng

@Composable
fun LiveLocationScreen(
    navController: NavHostController,
    viewModel: LiveLocationViewModel
) {
    val requestPermissionsAndStart = rememberLiveLocationPermissionFlow(
        onAllGranted = { viewModel.startSharing() }
    )

    LaunchedEffect(Unit) {
        // The realtime WS signal should keep this fresh, but poll as a fallback in case
        // that connection silently stalls (e.g. an idle connection dropped by a proxy).
        while (true) {
            viewModel.refreshFriendLocations()
            delay(20_000L)
        }
    }

    val entries = buildList {
        viewModel.selfPosition?.let { position ->
            add(
                LiveMapEntry(
                    label = appString(R.string.you),
                    position = position,
                    accuracyMeters = viewModel.selfAccuracyMeters,
                    trail = viewModel.selfTrail,
                    isSelf = true
                )
            )
        }
        viewModel.friendLocations.forEach { friend ->
            add(
                LiveMapEntry(
                    label = friend.user.name ?: friend.user.email,
                    position = LatLng(friend.latitude, friend.longitude),
                    accuracyMeters = friend.accuracyMeters,
                    trail = friend.trail.map { LatLng(it.latitude, it.longitude) }
                )
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LiveLocationMap(
            entries = entries,
            mapLoadError = appString(R.string.map_load_failed),
            modifier = Modifier.fillMaxSize()
        )

        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(appString(R.string.share_location), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (viewModel.isSharing) {
                            appString(R.string.friends_see_live_location)
                        } else {
                            appString(R.string.turned_off)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = viewModel.isSharing,
                    onCheckedChange = { checked ->
                        if (checked) {
                            requestPermissionsAndStart()
                        } else {
                            viewModel.stopSharing()
                        }
                    }
                )
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            )
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = appString(R.string.back)
                )
            }
        }

        viewModel.errorMessage?.let { message ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(12.dp),
                    color = Color(0xFFC62828),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
