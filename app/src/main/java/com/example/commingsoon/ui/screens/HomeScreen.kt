package com.example.commingsoon.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.commingsoon.R
import androidx.compose.ui.draw.clip
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.commingsoon.navigation.NavScreens
import com.example.commingsoon.viewmodels.JourneyViewModel
import com.example.commingsoon.viewmodels.Journey
import com.example.commingsoon.components.InteractiveWorldMap
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun HomeScreen (
    viewModel: JourneyViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.loadWorldMap(context)
    }

    var testSelectedCountryId by rememberSaveable { mutableStateOf<String?>(null) }
    var isFullscreen by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(isFullscreen) {
        if (isFullscreen) {
            val activity = context.findActivity()
            val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            onDispose {
                activity?.requestedOrientation = originalOrientation
            }
        } else {
            onDispose {}
        }
    }

    val isLight = MaterialTheme.colorScheme.background == Color.White
    val oceanColor = if (isLight) Color(0xFFD4F0FC) else Color(0xFF1E293B)
    val defaultCountryColor = if (isLight) Color(0xFFECECEC) else Color(0xFF334155)
    val borderColor = if (isLight) Color(0xFFCCCCCC) else Color(0xFF475569)

    val visitedCountryColor = MaterialTheme.colorScheme.primary
    val selectedCountryColor = MaterialTheme.colorScheme.secondary

    val customCountryColors = remember(
        viewModel.journeys,
        viewModel.countries,
        testSelectedCountryId,
        visitedCountryColor,
        selectedCountryColor
    ) {
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

        testSelectedCountryId?.let { selectedId ->
            colors[selectedId] = selectedCountryColor
        }
        colors
    }

    if (isFullscreen) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(oceanColor)
            ) {
                InteractiveWorldMap(
                    countries = viewModel.countries,
                    countryColors = customCountryColors,
                    zoomable = true,
                    oceanColor = oceanColor,
                    defaultCountryColor = defaultCountryColor,
                    borderColor = borderColor,
                    borderWidth = 0.4f,
                    modifier = Modifier.fillMaxSize(),
                    onCountrySelected = { clickedId ->
                        testSelectedCountryId = clickedId
                    }
                )

                IconButton(
                    onClick = { isFullscreen = false },
                    modifier = Modifier
                        .padding(24.dp)
                        .align(Alignment.TopStart)
                        .background(Color.White.copy(alpha = 0.8f), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Schließen",
                        tint = Color.Black
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // world map with countries visited marked
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(.33f)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (viewModel.countries.isEmpty()) {
                        Text(
                            text = "Loading Map...",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
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
                                .clickable { isFullscreen = true },
                            onCountrySelected = { _ ->
                                isFullscreen = true
                            }
                        )
                    }
                }
            }
        }

        // list of journeys
        LazyColumn(
            modifier = Modifier.weight(.67f).fillMaxSize()
        ) {
            itemsIndexed(viewModel.journeys) { index, journey ->
                JourneyCard(
                    journey = journey,
                    isFirst = index == 0,
                    isLast = index == viewModel.journeys.lastIndex,
                    onClick = { navController.navigate(
                        NavScreens.JourneyDetail.createRoute(journey.id)
                    ) }
                )
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
    }

    // Button at Bottom
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
            onClick = { navController.navigate(NavScreens.JourneyEditor.createRoute()) }
        ) {
            Text(text = stringResource(R.string.new_journey))
        }
    }
}

@Composable
fun Timeline(
    isFirst: Boolean,
    isLast: Boolean
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

            if (!isLast) {
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
        Box(
            modifier = Modifier
                .size(if (isFirst) 12.dp else 8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
fun JourneyCard (
    journey: Journey,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // TimeLine
            Timeline(isFirst, isLast)

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

        HorizontalDivider(
            color = Color.LightGray.copy(alpha = .3f),
            modifier = Modifier.padding(start = 50.dp)
        )
    }
}