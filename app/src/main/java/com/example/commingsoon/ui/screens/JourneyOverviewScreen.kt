package com.example.commingsoon.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.commingsoon.R
import com.example.commingsoon.language.appString
import com.example.commingsoon.navigation.NavScreens
import com.example.commingsoon.overlays.OverlayViewModel
import com.example.commingsoon.viewmodels.Journey
import com.example.commingsoon.viewmodels.JourneyViewModel
import com.example.commingsoon.components.InteractiveWorldMap
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun JourneyOverviewScreen (
    viewModel: JourneyViewModel,
    navController: NavController,
    overlayViewModel: OverlayViewModel
) {
    var mapExpanded by rememberSaveable { mutableStateOf(false) }
    var expandedJourneyId by rememberSaveable { mutableStateOf<Int?>(null) }
    var journeyToDelete by remember { mutableStateOf<Journey?>(null) }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.loadWorldMap(context)
    }

    // Map
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        AnimatedContent(
            targetState = mapExpanded,
            label = ""
        ) { expanded ->
            if (expanded) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .padding(16.dp)
                        .clickable {
                            mapExpanded = false
                        }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (viewModel.countries.isEmpty()) {
                            Text("Loading Map...")
                        } else {
                            val visitedCountryColor = MaterialTheme.colorScheme.primary
                            val customCountryColors = remember(viewModel.journeys, viewModel.countries, visitedCountryColor) {
                                val countryLatestEndDate = mutableMapOf<String, LocalDate>()
                                viewModel.journeys.forEach { journey ->
                                    journey.visitedCountries.forEach { countryNameOrId ->
                                        val svgId = viewModel.countries.find { country ->
                                            country.id.equals(countryNameOrId, ignoreCase = true) ||
                                            country.name?.equals(countryNameOrId, ignoreCase = true) == true
                                        }?.id
                                        if (svgId != null) {
                                            val existing = countryLatestEndDate[svgId]
                                            if (existing == null || journey.endDate.isAfter(existing)) {
                                                countryLatestEndDate[svgId] = journey.endDate
                                            }
                                        }
                                    }
                                }

                                val colors = mutableMapOf<String, Color>()
                                val today = LocalDate.now()
                                countryLatestEndDate.forEach { (svgId, endDate) ->
                                    val days = ChronoUnit.DAYS.between(endDate, today)
                                    val years = days / 365.25
                                    val opacity = if (years <= 1.0) {
                                        1.0f
                                    } else {
                                        kotlin.math.max(0.25f, 1.0f - 0.05f * (years.toFloat() - 1.0f))
                                    }
                                    colors[svgId] = visitedCountryColor.copy(alpha = opacity)
                                }
                                colors
                            }

                            val isLight = MaterialTheme.colorScheme.background == Color.White
                            val oceanColor = if (isLight) Color(0xFFD4F0FC) else Color(0xFF1E293B)
                            val defaultCountryColor = if (isLight) Color(0xFFECECEC) else Color(0xFF334155)
                            val borderColor = if (isLight) Color(0xFFCCCCCC) else Color(0xFF475569)

                            InteractiveWorldMap(
                                countries = viewModel.countries,
                                countryColors = customCountryColors,
                                oceanColor = oceanColor,
                                defaultCountryColor = defaultCountryColor,
                                borderColor = borderColor,
                                borderWidth = 0.4f,
                                zoomable = false,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(16.dp)
                        .clickable {
                            mapExpanded = true
                        }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tap to expand map",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        )

        // journey list
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(viewModel.journeys) { index, journey ->
                ExpandableJourneyCard(
                    journey = journey,
                    isExpanded = expandedJourneyId == journey.id,
                    isFirst = index == 0,
                    isLast = index == viewModel.journeys.lastIndex,
                    onClick = {
                        expandedJourneyId =
                            if (expandedJourneyId == journey.id)
                                null
                            else
                                journey.id
                    },
                    onChange = {
                        navController.navigate(
                            NavScreens.JourneyDetail.createRoute(journey.id)
                        )
                    },
                    onRemove = { journeyToDelete = journey },
                    onShare = { overlayViewModel.showJourneyShare(journey) }
                )
            }

            item {
                Spacer(Modifier.height(90.dp))
            }
        }

        // dialog for deleting journey
        if (journeyToDelete != null) {
            AlertDialog(
                onDismissRequest = {
                    journeyToDelete = null
                },
                title = {
                    Text(appString(R.string.remove_journey))
                },
                text = {
                    Text(appString(R.string.remove_journey_dialog, journeyToDelete!!.title))
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.removeJourney(journeyToDelete!!.id)
                            if (expandedJourneyId == journeyToDelete!!.id) {
                                expandedJourneyId = null
                            }
                            journeyToDelete = null
                        }
                    ) {
                        Text(appString(R.string.remove))
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            journeyToDelete = null
                        }
                    ) {
                        Text(appString(R.string.cancel))
                    }
                }
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(58.dp),
            shape = RoundedCornerShape(50),
            onClick = {
                navController.navigate(NavScreens.JourneyEditor.createRoute())
            }
        ) {
            Text(appString(R.string.new_journey))
        }
    }
}


@Composable
fun ExpandableJourneyCard(
    journey: Journey,
    isExpanded: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onChange: () -> Unit,
    onRemove: () -> Unit,
    onShare: () -> Unit
) {

    Column {
        JourneyListingCard(
            journey = journey,
            isFirst = isFirst,
            isLast = isLast,
            onClick = onClick,
            isExpanded = isExpanded
        )
        AnimatedVisibility(
            visible = isExpanded
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
//                TimelineJourney(
//                    isFirst = false,
//                    isLast = isLast
//                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 70.dp,
                            end = 16.dp,
                            bottom = 12.dp
                        ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onChange,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(Modifier.width(6.dp))

                        Text(
                            text = appString(R.string.show),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onRemove,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(Modifier.width(6.dp))

                        Text(
                            text = appString(R.string.remove),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onShare,
                        colors = ButtonDefaults.buttonColors(
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

@Composable
fun JourneyListingCard (
    journey: Journey,
    isFirst: Boolean,
    isLast: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // TimeLine
            TimelineJourney(isFirst, isLast, false)

            Row(
                modifier = Modifier
                    .padding(vertical = 15.dp)
                    .clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Spacer(Modifier.width(30.dp))

                // Dates
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

                // Journey Name
                Text(
                    text = journey.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(Modifier.width(24.dp))

                // Pins
                Text(
                    text = "${journey.pinCount} Pins",
                    modifier = Modifier.align(Alignment.Bottom),
                    color = Color.Gray.copy(alpha = .7f)
                )

            }
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
fun TimelineJourney(
    isFirst: Boolean,
    isLast: Boolean,
    isSmallDot: Boolean = true
) {
    Box(
        modifier = Modifier
            .width(24.dp)
            .height(80.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.primary)
                )
            } else {
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(1.dp))

            if (!isLast && !isSmallDot) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.primary)
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        }
        if (!isSmallDot) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}