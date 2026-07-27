package com.example.comingsoon.ui.screens

import android.content.res.Configuration
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.comingsoon.R
import com.example.comingsoon.language.appString
import com.example.comingsoon.language.appDateString
import com.example.comingsoon.navigation.NavScreens
import com.example.comingsoon.overlays.OverlayViewModel
import com.example.comingsoon.viewmodels.JourneyLocation
import com.example.comingsoon.viewmodels.JourneyViewModel
import com.example.comingsoon.components.InteractiveWorldMap
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun JourneyDetailScreen (
    journeyId: Int,
    ownerId: Int? = null,
    viewModel: JourneyViewModel,
    navController: NavController,
    overlayViewModel: OverlayViewModel
) {
    val journey = if (ownerId == null) {
        viewModel.getJourney(journeyId)
    } else {
        viewModel.getSharedJourney(ownerId, journeyId)
    } ?: return
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var mapExpanded by rememberSaveable { mutableStateOf(isLandscape) }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.loadWorldMap(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 20.dp)
    ) {
        // Titel & Date
        Text(
            text = journey.title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "${appDateString(journey.startDate)} – ${appDateString(journey.endDate)}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(16.dp))

        // Map Placeholder
        // composable from HomeScreen.kt
        if (isLandscape || mapExpanded) {
            ExpandableMap(
                expanded = mapExpanded,
                onExpandedChange = {
                    mapExpanded = it
                },
                content = {
                    JourneyVisitedCountriesMap(
                        journey = journey,
                        countries = viewModel.countries,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )
        } else {
            
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
                JourneyVisitedCountriesMap(
                    journey = journey,
                    countries = viewModel.countries,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        }

        Spacer(Modifier.height(16.dp))

        // Pins
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(journey.locations) { location ->
                PinCard(location)
            }

            item {
                Spacer(Modifier.height(90.dp))
            }
        }
    }

    if (journey.isOwned) Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(58.dp)
                .clip(RoundedCornerShape(50))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(50)
                )
        ) {

            // Edit
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.background)
                    .clickable {
                        navController.navigate(
                            NavScreens.JourneyEditor.createRoute(journey.id)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = appString(R.string.edit),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // border
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = .3f)
                    )
            )

            // Share
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        overlayViewModel.showJourneyShare(journey)
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.background,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = appString(R.string.share),
                        color = MaterialTheme.colorScheme.background,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun JourneyVisitedCountriesMap(
    journey: com.example.comingsoon.viewmodels.Journey,
    countries: List<com.example.comingsoon.viewmodels.MapCountry>,
    modifier: Modifier = Modifier
) {
    if (countries.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator()
        }
        return
    }

    val visitedCountryColor = MaterialTheme.colorScheme.primary
    val countryColors = remember(
        journey.visitedCountries,
        countries,
        visitedCountryColor,
        journey.endDate
    ) {
        val today = LocalDate.now()
        val days = ChronoUnit.DAYS.between(journey.endDate, today)
        val years = days / 365.25
        val opacity = if (years <= 1.0) {
            1.0f
        } else {
            kotlin.math.max(0.25f, 1.0f - 0.05f * (years.toFloat() - 1.0f))
        }
        val colorWithOpacity = visitedCountryColor.copy(alpha = opacity)

        buildMap {
            journey.visitedCountries.forEach { countryNameOrId ->
                countries.firstOrNull { country ->
                    country.id.equals(countryNameOrId, ignoreCase = true) ||
                        country.name?.equals(countryNameOrId, ignoreCase = true) == true
                }?.id?.let { put(it, colorWithOpacity) }
            }
        }
    }

    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    InteractiveWorldMap(
        countries = countries,
        countryColors = countryColors,
        oceanColor = if (isLight) Color(0xFFD4F0FC) else Color(0xFF1E293B),
        defaultCountryColor = if (isLight) Color(0xFFECECEC) else Color(0xFF334155),
        borderColor = if (isLight) Color(0xFFCCCCCC) else Color(0xFF475569),
        borderWidth = 0.4f,
        zoomable = false,
        modifier = modifier.padding(8.dp)
    )
}

@Composable
fun PinCard(
    location: JourneyLocation
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = location.name,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = appString(R.string.latitude, location.latitude),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Text(
                text = appString(R.string.longitude, location.longitude),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }

    HorizontalDivider(
        color = Color.LightGray.copy(alpha = .3f),
        modifier = Modifier.padding(start = 56.dp)
    )
}
