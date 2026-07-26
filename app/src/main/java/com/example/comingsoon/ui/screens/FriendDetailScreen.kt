package com.example.comingsoon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.comingsoon.R
import com.example.comingsoon.language.appString
import com.example.comingsoon.navigation.NavScreens
import com.example.comingsoon.viewmodels.FriendJourneyTab
import com.example.comingsoon.viewmodels.FriendViewModel
import com.example.comingsoon.viewmodels.Journey
import com.example.comingsoon.viewmodels.JourneyViewModel
import com.example.comingsoon.sync.JourneyShareSnapshot

@Composable
fun FriendDetailScreen(
    friendId: Int,
    friendViewModel: FriendViewModel,
    journeyViewModel: JourneyViewModel,
    navController: NavHostController
) {

    val friend = friendViewModel.getFriend(friendId) ?: return

    var selectedTab by rememberSaveable { mutableStateOf(FriendJourneyTab.SHARED_BY_ME) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = friend.name,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 20.dp
                ),
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    appString(R.string.map_placeholder)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        JourneyTabSwitch(
            modifier = Modifier.padding(horizontal = 16.dp),
            selected = selectedTab,
            onSelected = {
                selectedTab = it
            }
        )

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            val shares =
                if (selectedTab == FriendJourneyTab.SHARED_BY_ME) {
                    journeyViewModel.sharesByMeWith(friend.id)
                } else {
                    journeyViewModel.sharesWithMeBy(friend.id)
                }
            itemsIndexed(shares) { index, share ->
                FriendJourneyCard(
                    share = share,
                    isLast = index == shares.lastIndex,
                    onClick = {
                        if (selectedTab == FriendJourneyTab.SHARED_BY_ME) {
                            val ownJourney = journeyViewModel.journeys.firstOrNull {
                                it.serverId == share.journey.serverId
                            }
                            if (ownJourney != null) {
                                navController.navigate(
                                    NavScreens.JourneyDetail.createRoute(ownJourney.id)
                                )
                            }
                        } else {
                            navController.navigate(
                                NavScreens.SharedJourneyDetail.createRoute(
                                    share.ownerId,
                                    requireNotNull(share.journey.serverId)
                                )
                            )
                        }
                    }
                )
            }

            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun JourneyTabSwitch(
    modifier: Modifier = Modifier,
    selected: FriendJourneyTab,
    onSelected: (FriendJourneyTab) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(50))
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
                    if (selected == FriendJourneyTab.SHARED_BY_ME) {
                        selectedColor
                    } else {
                        Color.Transparent
                    }
                )
                .clickable { onSelected(FriendJourneyTab.SHARED_BY_ME) },
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = appString(R.string.shared_by_me),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    if (selected == FriendJourneyTab.SHARED_WITH_ME) {
                        selectedColor
                    } else {
                        Color.Transparent
                    }
                )
                .clickable { onSelected(FriendJourneyTab.SHARED_WITH_ME) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = appString(R.string.shared_with_me),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun FriendJourneyCard(
    share: JourneyShareSnapshot,
    isLast: Boolean,
    onClick: () -> Unit
) {
    val journey = share.journey
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    horizontal = 16.dp,
                    vertical = 15.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = journey.startDate.toString(),
                    color = Color.Gray.copy(alpha = .7f),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "-",
                    color = Color.Gray.copy(alpha = .7f),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = journey.endDate.toString(),
                    color = Color.Gray.copy(alpha = .7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.width(30.dp))

            Text(
                text = journey.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.width(20.dp))

            Text(
                text = "${journey.pinCount} Pins",
                color = Color.Gray.copy(alpha = .7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            text = if (share.shareType == "automatic") {
                appString(R.string.automatic_share_metadata, share.ownerName)
            } else {
                appString(
                    R.string.manual_share_metadata,
                    share.ownerName,
                    share.sharedAt.take(10)
                )
            },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall
        )

        if (!isLast) {
            HorizontalDivider(
                color = Color.LightGray.copy(alpha = .3f),
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
