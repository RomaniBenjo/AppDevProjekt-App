package com.example.commingsoon.overlays

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.commingsoon.viewmodels.Friend
import com.example.commingsoon.R
import com.example.commingsoon.language.appString
import com.example.commingsoon.viewmodels.Journey

enum class ShareTab {
    FRIENDS,
    QR_CODE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareJourneyOverlay(
    journey: Journey,
    friends: List<Friend>,
    onDismiss: () -> Unit,
    onShare: (Friend) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentTab by rememberSaveable { mutableStateOf(ShareTab.FRIENDS) }

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

            // switch at top
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
                text = journey.title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = appString(R.string.share_journey),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            ShareModeSwitch(
                selected = currentTab,
                onSelected = {
                    currentTab = it
                },
                leftTitle = appString(R.string.friends),
                rightTitle = appString(R.string.qr_code)
            )

            when (currentTab) {
                ShareTab.FRIENDS -> {
                    FriendShareList(
                        friends = friends,
                        onShare = onShare
                    )
                }
                ShareTab.QR_CODE -> {
                    QrCodeView(
                        journey = journey
                    )
                }
            }
        }
    }
}

@Composable
fun ShareModeSwitch(
    selected: ShareTab,
    onSelected: (ShareTab) -> Unit,
    leftTitle: String,
    rightTitle: String
) {

    Row(
        modifier = Modifier
            .padding(vertical = 20.dp)
            .fillMaxWidth()
            .height(56.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline,
                RoundedCornerShape(50)
            )
    ) {
        val selectedColor = MaterialTheme.colorScheme.secondary
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    if (selected == ShareTab.FRIENDS)
                        selectedColor
                    else
                        Color.Transparent
                )
                .clickable { onSelected(ShareTab.FRIENDS) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = leftTitle,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    if (selected == ShareTab.QR_CODE) {
                        selectedColor
                    } else { Color.Transparent }
                )
                .clickable { onSelected(ShareTab.QR_CODE) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rightTitle,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun FriendShareList(
    friends: List<Friend>,
    onShare: (Friend) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 450.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(friends) { friend ->
            FriendShareItem(
                friend = friend,
                onShare = { onShare(friend) }
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
fun FriendShareItem(
    friend: Friend,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(
                friend.image ?: R.drawable.profile_placeholder
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
            onClick = onShare,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.background
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )

            Spacer(Modifier.width(6.dp))

            Text(appString(R.string.share))
        }
    }
}

@Composable
fun QrCodeView(
    journey: Journey
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = journey.title,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = appString(R.string.share_journey_qr_description),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(Modifier.height(32.dp))

        Card(
            modifier = Modifier.size(250.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.qr_placeholder), // TODO: creating real QR codes
                    contentDescription = appString(R.string.qr_code),
                    modifier = Modifier.size(190.dp)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = appString(R.string.friends_can_scan),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(Modifier.height(28.dp))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(50),
            onClick = {
                // TODO Save / Share QR
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.Download,
                contentDescription = null
            )

            Spacer(Modifier.width(8.dp))

            Text(appString(R.string.save_qr_code))
        }

        Spacer(Modifier.height(20.dp))
    }
}