package com.example.comingsoon.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.comingsoon.R
import com.example.comingsoon.language.appString
import com.example.comingsoon.navigation.NavScreens
import com.example.comingsoon.overlays.OverlayViewModel
import com.example.comingsoon.viewmodels.Friend
import com.example.comingsoon.viewmodels.FriendViewModel
import com.example.comingsoon.viewmodels.FriendRequest
import com.example.comingsoon.components.FriendAvatar

@Composable
fun FriendOverviewScreen (
    viewModel: FriendViewModel,
    navController: NavHostController,
    overlayViewModel: OverlayViewModel
) {
    var expandedFriendId by rememberSaveable { mutableStateOf<Int?>(null) }
    var friendToRemove by remember { mutableStateOf<Friend?>(null) }
    LaunchedEffect(Unit) { viewModel.refresh() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 20.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            viewModel.errorMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            if (viewModel.isLoading && viewModel.friends.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
            }

            if (viewModel.incomingRequests.isNotEmpty()) {
                item { FriendSectionTitle(appString(R.string.incoming_requests)) }
                items(viewModel.incomingRequests, key = { "incoming-${it.id}" }) { request ->
                    FriendRequestRow(
                        request = request,
                        incoming = true,
                        onAccept = { viewModel.acceptRequest(request.id) },
                        onDelete = { viewModel.deleteRequest(request.id) }
                    )
                }
            }

            if (viewModel.outgoingRequests.isNotEmpty()) {
                item { FriendSectionTitle(appString(R.string.outgoing_requests)) }
                items(viewModel.outgoingRequests, key = { "outgoing-${it.id}" }) { request ->
                    FriendRequestRow(
                        request = request,
                        incoming = false,
                        onAccept = {},
                        onDelete = { viewModel.deleteRequest(request.id) }
                    )
                }
            }

            item { FriendSectionTitle(appString(R.string.your_friends)) }
            if (!viewModel.isLoading && viewModel.friends.isEmpty()) {
                item {
                    Text(
                        text = appString(R.string.no_friends_yet),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
            items(viewModel.friends) { friend ->
                ExpandableFriendCard(
                    friend = friend,
                    isExpanded = expandedFriendId == friend.id,
                    onClick = {
                        expandedFriendId =
                            if (expandedFriendId == friend.id) { null }
                            else { friend.id }
                    },
                    onShow = { navController.navigate(
                        NavScreens.FriendDetail.createRoute(friend.id)
                    )},
                    onRemove = { friendToRemove = friend },
                    onShare = {
                        overlayViewModel.showFriendShare(friend)
                    }
                )
            }

            item {
                Spacer(Modifier.height(90.dp))
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(50))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(50)
                )
        ) {

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.background)
                    .clickable {
                        overlayViewModel.showAddFriend()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.PersonAdd,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = appString(R.string.new_friend),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = .3f))
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        navController.navigate(
                            NavScreens.LiveLocations.route
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        Icons.Outlined.LocationOn,
                        null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(appString(R.string.live_location))
                }
            }
        }
    }

    friendToRemove?.let { friend ->
        AlertDialog(
            onDismissRequest = { friendToRemove = null },
            title = { Text(appString(R.string.remove_friend)) },
            text = {
                Text(appString(R.string.remove_friend_dialog, friend.name))
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeFriend(friend)
                        if (expandedFriendId == friend.id) {
                            expandedFriendId = null
                        }
                        friendToRemove = null
                    }
                ) { Text(appString(R.string.remove)) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { friendToRemove = null }
                ) { Text(appString(R.string.cancel)) }
            }
        )
    }
}

@Composable
fun ExpandableFriendCard(
    friend: Friend,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onShow: () -> Unit,
    onRemove: () -> Unit,
    onShare: () -> Unit
) {

    Column {
        FriendCard(
            friend = friend,
            isExpanded = isExpanded,
            onClick = onClick
        )

        AnimatedVisibility(
            visible = isExpanded
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 80.dp,
                        end = 16.dp,
                        bottom = 12.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(.9f),
                    onClick = onShow
                ) {
                    BoxWithConstraints {
                        val showText = maxWidth > 90.dp

                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )

                        if (showText) {
                            Spacer(Modifier.width(4.dp))

                            Text(
                                text = appString(R.string.show),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onRemove
                ) {
                    BoxWithConstraints {
                        val showText = maxWidth > 90.dp

                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )

                        if (showText) {
                            Spacer(Modifier.width(4.dp))

                            Text(
                                text = appString(R.string.remove),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Button(
                    modifier = Modifier.weight(.9f),
                    onClick = onShare
                ) {
                    BoxWithConstraints {
                        val showText = maxWidth > 90.dp

                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )

                        if (showText) {
                            Spacer(Modifier.width(6.dp))

                            Text(
                                text = appString(R.string.share),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendCard(
    friend: Friend,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FriendAvatar(friend = friend, modifier = Modifier.size(56.dp))

            Spacer(Modifier.width(16.dp))

            Text(
                text = friend.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium
            )
        }
        if (!isExpanded) {
            HorizontalDivider(
                color = Color.LightGray.copy(alpha = .3f),
                modifier = Modifier.padding(start = 50.dp)
            )
        }
    }
}

@Composable
private fun FriendSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp)
    )
}

@Composable
private fun FriendRequestRow(
    request: FriendRequest,
    incoming: Boolean,
    onAccept: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FriendAvatar(friend = request.user, modifier = Modifier.size(48.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(request.user.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    request.user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (incoming) {
                Button(onClick = onAccept) { Text(appString(R.string.accept)) }
                Spacer(Modifier.width(6.dp))
            }
            OutlinedButton(onClick = onDelete) {
                Text(appString(if (incoming) R.string.reject else R.string.cancel_request))
            }
        }
    }
}
