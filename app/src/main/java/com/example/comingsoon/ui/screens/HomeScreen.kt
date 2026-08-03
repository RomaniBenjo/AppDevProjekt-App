package com.example.comingsoon.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.comingsoon.R
import com.example.comingsoon.language.appString
import com.example.comingsoon.language.appQuantityString
import com.example.comingsoon.language.appDateString
import com.example.comingsoon.language.LocalAppLanguage
import com.example.comingsoon.language.localizedCountryName
import com.example.comingsoon.navigation.NavScreens
import com.example.comingsoon.viewmodels.Journey
import com.example.comingsoon.viewmodels.JourneyViewModel
import com.example.comingsoon.viewmodels.CountryViewModel
import com.example.comingsoon.components.InteractiveWorldMap
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Arrangement
import com.example.comingsoon.viewmodels.ClaimStatus

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
    countryViewModel: CountryViewModel,
    navController: NavController
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var mapExpanded by rememberSaveable { mutableStateOf(isLandscape) }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        countryViewModel.loadWorldMap()
    }

    var testSelectedCountryId by rememberSaveable { mutableStateOf<String?>(null) }
    var isFullscreen by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                          permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                countryViewModel.claimCurrentCountry(context)
            } else {
                countryViewModel.resetClaimStatus()
            }
        }
    )

    val onClaimClick = {
        val fineGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarseGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            countryViewModel.claimCurrentCountry(context)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

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

    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val oceanColor = if (isLight) Color(0xFFD4F0FC) else Color(0xFF1E293B)
    val defaultCountryColor = if (isLight) Color(0xFFECECEC) else Color(0xFF334155)
    val borderColor = if (isLight) Color(0xFFCCCCCC) else Color(0xFF475569)

    val visitedCountryColor = MaterialTheme.colorScheme.primary
    val selectedCountryColor = MaterialTheme.colorScheme.secondary

    val customCountryColors = remember(
        viewModel.journeys,
        countryViewModel.countries,
        countryViewModel.claimedCountries.toList(),
        testSelectedCountryId,
        visitedCountryColor,
        selectedCountryColor
    ) {
        val countryLatestEndDate = mutableMapOf<String, LocalDate>()
        viewModel.journeys.forEach { journey ->
            journey.visitedCountries.forEach { countryNameOrId ->
                val svgId = countryViewModel.countries.find { country ->
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

        // Highlight claimed countries in gold/amber
        countryViewModel.claimedCountries.forEach { svgId ->
            colors[svgId] = Color(0xFFFFB300)
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
                    countries = countryViewModel.countries,
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
                        contentDescription = appString(R.string.close),
                        tint = Color.Black
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isLandscape || mapExpanded) {
            ExpandableMap(
                expanded = mapExpanded,
                onExpandedChange = {
                    mapExpanded = it
                },
                content = {
                    if (countryViewModel.countries.isEmpty()) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        InteractiveWorldMap(
                            countries = countryViewModel.countries,
                            countryColors = customCountryColors,
                            oceanColor = oceanColor,
                            defaultCountryColor = defaultCountryColor,
                            borderColor = borderColor,
                            borderWidth = 0.4f,
                            zoomable = false,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            onCountrySelected = { clickedId ->
                                testSelectedCountryId = clickedId
                            },
                            onMapTapped = {
                                isFullscreen = true
                            }
                        )
                    }
                }
            )
        } else {
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
                        if (countryViewModel.countries.isEmpty()) {
                            Text(
                                text = appString(R.string.loading_map),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            InteractiveWorldMap(
                                countries = countryViewModel.countries,
                                countryColors = customCountryColors,
                                oceanColor = oceanColor,
                                defaultCountryColor = defaultCountryColor,
                                borderColor = borderColor,
                                borderWidth = 0.4f,
                                zoomable = false,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                onMapTapped = {
                                    isFullscreen = true
                                }
                            )
                        }
                    }
                }
            }
        }

        // list of journeys
        LazyColumn(
            modifier = Modifier.weight(.67f).fillMaxSize()
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = appString(R.string.claim_location),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = appString(R.string.claim_countries),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Text(
                            text = appString(R.string.claim_explanation),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        
                        Spacer(Modifier.height(12.dp))

                        when (val status = countryViewModel.claimStatus) {
                            is ClaimStatus.Idle -> {
                                Button(
                                    onClick = onClaimClick,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(appString(R.string.claim_current_location))
                                }
                            }
                            is ClaimStatus.Detecting -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Text(appString(R.string.detecting_location), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            is ClaimStatus.Success -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = if (status.isNew)
                                                appString(R.string.claim_success, status.countryName)
                                            else
                                                appString(R.string.claim_already_exists, status.countryName),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF2E7D32)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Button(
                                            onClick = { countryViewModel.resetClaimStatus() },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text(appString(R.string.ok))
                                        }
                                    }
                                }
                            }
                            is ClaimStatus.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = appString(R.string.claim_error, status.message),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFC62828)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Row(modifier = Modifier.align(Alignment.End)) {
                                            Button(
                                                onClick = { countryViewModel.resetClaimStatus() },
                                                colors = androidx.compose.material3.ButtonDefaults.textButtonColors()
                                            ) {
                                                Text(appString(R.string.cancel))
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Button(
                                                onClick = onClaimClick
                                            ) {
                                                Text(appString(R.string.try_again))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (countryViewModel.claimedCountries.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            val language = LocalAppLanguage.current
                            val names = remember(
                                countryViewModel.claimedCountries.toList(),
                                countryViewModel.countries,
                                language
                            ) {
                                val locale = Locale.forLanguageTag(language.languageTag)
                                countryViewModel.claimedCountries.map { code ->
                                    val country = countryViewModel.countries.find {
                                        it.id.equals(code, ignoreCase = true)
                                    }
                                    localizedCountryName(code, country?.name, locale)
                                }.joinToString(", ")
                            }
                            Text(
                                text = appString(R.string.claimed_countries, names),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

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
            Text(text = appString(R.string.new_journey))
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

                // Journey Name
                Text(
                    text = journey.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(Modifier.width(24.dp))

                // Pins
                Text(
                    text = appQuantityString(R.plurals.pin_count, journey.pinCount),
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

@Composable
fun ExpandableMap(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .animateContentSize()
    ) {
        // Header
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onExpandedChange(!expanded)
            },
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(
                topStart = 28.dp,
                topEnd = 28.dp,
                bottomStart = if (expanded) 0.dp else 28.dp,
                bottomEnd = if (expanded) 0.dp else 28.dp
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                if (expanded)
                    appString(R.string.hide_map)
                else
                    appString(R.string.show_map)
            )

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector =
                    if (expanded)
                        Icons.Default.KeyboardArrowUp
                    else
                        Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
        }

        AnimatedContent(
            targetState = expanded,
            label = ""
        ) { isExpanded ->
            if (isExpanded) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    shape = RoundedCornerShape(
                        topStart = 0.dp,
                        topEnd = 0.dp,
                        bottomStart = 28.dp,
                        bottomEnd = 28.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                        content = content
                    )
                }
            }
        }
    }
}
