package com.example.commingsoon.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.commingsoon.viewmodels.Friend
import com.example.commingsoon.viewmodels.FriendViewModel
import com.example.commingsoon.R
import com.example.commingsoon.navigation.NavScreens
import com.example.commingsoon.overlays.OverlayViewModel

@Composable
fun FriendOverviewScreen (
    viewModel: FriendViewModel,
    navController: NavHostController,
    overlayViewModel: OverlayViewModel
) {
    var expandedFriendId by rememberSaveable { mutableStateOf<Int?>(null) }
    var friendToRemove by remember { mutableStateOf<Friend?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 20.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
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
                        "Add Friend",
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
                        // TODO Live Location
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

                    Text("Live Location")
                }
            }
        }
    }

    friendToRemove?.let { friend ->
        AlertDialog(
            onDismissRequest = { friendToRemove = null },
            title = { Text("Remove Friend") },
            text = {
                Text("Are you sure you want to remove ${friend.name}?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeFriend(friend.id)
                        if (expandedFriendId == friend.id) {
                            expandedFriendId = null
                        }
                        friendToRemove = null
                    }
                ) { Text("Remove") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { friendToRemove = null }
                ) { Text("Cancel") }
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onShow
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(Modifier.width(6.dp))

                    Text("Show")
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onRemove
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        null,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(Modifier.width(6.dp))

                    Text("Remove")
                }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onShare
                ) {
                    Icon(
                        Icons.Outlined.Share,
                        null,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(Modifier.width(6.dp))

                    Text("Share")
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
            Image(
                painter = painterResource(
                    friend.image ?: R.drawable.profile_placeholder
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
            )

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