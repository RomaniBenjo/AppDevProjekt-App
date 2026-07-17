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

    var testSelectedCountryId by rememberSaveable { mutableStateOf<String?>("United States") }
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

    // Option B: Wir erstellen eine Map von Länder-IDs zu Compose-Farben
    val customCountryColors = remember(testSelectedCountryId) {
        val colors = mutableMapOf(
            "United States" to Color(0xFFFF5722), // Orange/Rot (USA wird über den class-Namen gefärbt)
            "DE" to Color(0xFF4CAF50), // Grün
            "FR" to Color(0xFFFFC107)  // Gelb
        )
        testSelectedCountryId?.let { selectedId ->
            colors[selectedId] = Color.Blue // Das ausgewählte Land wird blau markiert
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
                    .background(Color(0xFFD4F0FC))
            ) {
                InteractiveWorldMap(
                    countries = viewModel.countries,
                    countryColors = customCountryColors,
                    zoomable = true,
                    oceanColor = Color(0xFFD4F0FC),
                    borderColor = Color(0xFF222222),
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
                            countryColors = customCountryColors, // Die Farb-Map übergeben
                            oceanColor = Color(0xFFD4F0FC),      // Individuelle Ozeanfarbe (Hellblau)
                            borderColor = Color(0xFF222222),     // Randfarbe (Dunkelgrau)
                            borderWidth = 0.4f,                  // Randstärke
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