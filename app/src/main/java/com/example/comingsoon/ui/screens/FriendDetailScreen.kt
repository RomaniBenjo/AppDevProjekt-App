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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.comingsoon.R
import com.example.comingsoon.components.InteractiveWorldMap
import com.example.comingsoon.language.appString
import com.example.comingsoon.language.appQuantityString
import com.example.comingsoon.language.appDateString
import com.example.comingsoon.navigation.NavScreens
import com.example.comingsoon.sync.JourneyShareSnapshot
import com.example.comingsoon.viewmodels.FriendJourneyTab
import com.example.comingsoon.viewmodels.FriendViewModel
import com.example.comingsoon.viewmodels.Journey
import com.example.comingsoon.viewmodels.JourneyViewModel
import com.example.comingsoon.viewmodels.JourneyShareViewModel
import com.example.comingsoon.viewmodels.CountryViewModel
import com.example.comingsoon.viewmodels.MapCountry
import java.text.Normalizer
import java.util.Locale

@Composable
fun FriendDetailScreen(
    friendId: Int,
    friendViewModel: FriendViewModel,
    journeyViewModel: JourneyViewModel,
    journeyShareViewModel: JourneyShareViewModel,
    countryViewModel: CountryViewModel,
    navController: NavHostController
) {

    val friend = friendViewModel.getFriend(friendId) ?: return

    var selectedTab by rememberSaveable { mutableStateOf(FriendJourneyTab.SHARED_BY_ME) }
    val sharedByMe = journeyShareViewModel.sharesByMeWith(
        friend.id,
        journeyViewModel.journeys
    )
    val sharedWithMe = journeyShareViewModel.sharesWithMeBy(friend.id)
    LaunchedEffect(Unit) {
        countryViewModel.loadWorldMap()
    }

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

        FriendJourneyWorldMap(
            friendName = friend.name,
            countries = countryViewModel.countries,
            journeysSharedByMe = sharedByMe.map(JourneyShareSnapshot::journey),
            journeysSharedWithMe = sharedWithMe.map(JourneyShareSnapshot::journey)
        )

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
                    sharedByMe
                } else {
                    sharedWithMe
                }
            itemsIndexed(
                items = shares,
                key = { _, share ->
                    "${share.ownerId}:${share.recipientId}:${share.journey.serverId}"
                }
            ) { index, share ->
                FriendJourneyCard(
                    share = share,
                    isLast = index == shares.lastIndex,
                    onClick = {
                        if (selectedTab == FriendJourneyTab.SHARED_BY_ME) {
                            val ownJourney = share.localJourneyId?.let(
                                journeyViewModel::getJourney
                            ) ?: journeyViewModel.journeys.firstOrNull {
                                share.journey.serverId != null &&
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
private fun FriendJourneyWorldMap(
    friendName: String,
    countries: List<MapCountry>,
    journeysSharedByMe: List<Journey>,
    journeysSharedWithMe: List<Journey>
) {
    var selectedCountryId by rememberSaveable { mutableStateOf<String?>(null) }
    val myCountryIds = remember(countries, journeysSharedByMe) {
        resolveCountryIds(countries, journeysSharedByMe)
    }
    val friendCountryIds = remember(countries, journeysSharedWithMe) {
        resolveCountryIds(countries, journeysSharedWithMe)
    }
    val commonCountryIds = remember(myCountryIds, friendCountryIds) {
        myCountryIds intersect friendCountryIds
    }

    val myColor = MaterialTheme.colorScheme.primary
    val friendColor = MaterialTheme.colorScheme.tertiary
    val commonColor = MaterialTheme.colorScheme.secondary
    val colors = remember(
        myCountryIds,
        friendCountryIds,
        commonCountryIds,
        myColor,
        friendColor,
        commonColor
    ) {
        buildMap {
            myCountryIds.forEach { put(it, myColor) }
            friendCountryIds.forEach { put(it, friendColor) }
            commonCountryIds.forEach { put(it, commonColor) }
        }
    }

    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val oceanColor = if (isLight) Color(0xFFD4F0FC) else Color(0xFF1E293B)
    val defaultCountryColor = if (isLight) Color(0xFFECECEC) else Color(0xFF334155)
    val borderColor = if (isLight) Color(0xFFCCCCCC) else Color(0xFF475569)
    val selectedCountryName = countries
        .firstOrNull { it.id == selectedCountryId }
        ?.let { it.name ?: it.id }
    val mapDescription = appString(R.string.friend_map_content_description, friendName)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when {
                    countries.isEmpty() -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = appString(R.string.friend_map_loading),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    else -> {
                        InteractiveWorldMap(
                            countries = countries,
                            countryColors = colors,
                            oceanColor = oceanColor,
                            defaultCountryColor = defaultCountryColor,
                            borderColor = borderColor,
                            borderWidth = 0.4f,
                            zoomable = false,
                            onCountrySelected = { selectedCountryId = it },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .semantics {
                                    contentDescription = mapDescription
                                }
                        )

                        if (colors.isEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = appString(R.string.friend_map_empty),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        selectedCountryName?.let { name ->
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = name,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FriendMapLegendItem(
                    modifier = Modifier.weight(1f),
                    color = myColor,
                    text = appString(R.string.friend_map_my_trips)
                )
                FriendMapLegendItem(
                    modifier = Modifier.weight(1f),
                    color = friendColor,
                    text = appString(R.string.friend_map_friend_trips, friendName)
                )
                FriendMapLegendItem(
                    modifier = Modifier.weight(1f),
                    color = commonColor,
                    text = appString(R.string.friend_map_both)
                )
            }
        }
    }
}

@Composable
private fun FriendMapLegendItem(
    color: Color,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private fun resolveCountryIds(
    countries: List<MapCountry>,
    journeys: List<Journey>
): Set<String> {
    val countryIdsByToken = buildMap {
        countries.forEach { country ->
            put(normalizeCountryToken(country.id), country.id)
            country.name?.let { put(normalizeCountryToken(it), country.id) }
        }
    }

    return journeys
        .asSequence()
        .flatMap { it.visitedCountries.asSequence() }
        .mapNotNull { countryIdsByToken[normalizeCountryToken(it)] }
        .toSet()
}

private fun normalizeCountryToken(value: String): String =
    Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .lowercase(Locale.ROOT)

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
                    text = appDateString(journey.startDate),
                    color = Color.Gray.copy(alpha = .7f),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "-",
                    color = Color.Gray.copy(alpha = .7f),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = appDateString(journey.endDate),
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
                text = appQuantityString(R.plurals.pin_count, journey.pinCount),
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
