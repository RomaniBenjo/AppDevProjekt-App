package com.example.commingsoon.overlays

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.commingsoon.viewmodels.Friend
import com.example.commingsoon.viewmodels.FriendViewModel
import com.example.commingsoon.R
import com.example.commingsoon.R.drawable.profile_placeholder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendOverlay(
    viewModel: FriendViewModel,
    onDismiss: () -> Unit,
    onAddFriend: (Friend) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    var currentTab by rememberSaveable {
        mutableStateOf(ShareTab.FRIENDS)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
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

            Text(
                text = "Add Friend",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Search for a friend or scan a QR code",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            ShareModeSwitch(
                selected = currentTab,
                onSelected = { currentTab = it },
                leftTitle = "Search",
                rightTitle = "QR Code"
            )

            when (currentTab) {
                ShareTab.FRIENDS ->
                    FriendSearchView(
                        viewModel = viewModel,
                        onAddFriend = onAddFriend
                    )

                ShareTab.QR_CODE ->
                    ScanQrPlaceholder()
            }
        }
    }
}

@Composable
fun FriendSearchView(
    viewModel: FriendViewModel,
    onAddFriend: (Friend) -> Unit
) {

    var search by rememberSaveable {
        mutableStateOf("")
    }
    var result by remember {
        mutableStateOf<List<Friend>>(emptyList())
    }

    Column {
        Row {
            TextField(
                value = search,
                onValueChange = {
                    search = it
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = {
                    Text("Friend Name")
                }
            )

            Spacer(Modifier.width(12.dp))

            Button(
                onClick = {
                    result = viewModel.searchFriends(search)
                }
            ) {
                Icon(
                    Icons.Outlined.Search,
                    null
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        LazyColumn {
            items(result) { friend ->
                FriendSearchItem(
                    friend = friend,
                    onAdd = {
                        onAddFriend(friend)
                    }
                )
                HorizontalDivider(
                    color = Color.LightGray.copy(alpha = .3f),
                    modifier = Modifier.padding(start = 50.dp)
                )
            }
        }
    }
}

@Composable
fun FriendSearchItem(
    friend: Friend,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(
                friend.image ?: profile_placeholder
            ),
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
        )

        Spacer(Modifier.width(14.dp))

        Text(
            text = friend.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium
        )

        FilledTonalButton(
            onClick = onAdd
        ) {

            Icon(
                Icons.Outlined.PersonAdd,
                null,
                modifier = Modifier.size(16.dp)
            )

            Spacer(Modifier.width(6.dp))

            Text("Add")
        }
    }
}

@Composable
fun ScanQrPlaceholder() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Card(
            shape = RoundedCornerShape(24.dp)
        ) {

            Image(
                painter = painterResource(R.drawable.qr_placeholder), // TODO: creating real QR codes
                contentDescription = null,
                modifier = Modifier.size(220.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Scan a friend's QR code to add them.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}