package com.example.commingsoon.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.commingsoon.viewmodels.Journey
import com.example.commingsoon.viewmodels.JourneyLocation
import com.example.commingsoon.viewmodels.JourneyViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant

@Composable
fun JourneyEditorScreen (
    viewModel: JourneyViewModel,
    journey: Journey? = null,
    onDiscard: () -> Unit,
    onSave: (Journey) -> Unit
) {
    val isEditing = journey != null
    var title by rememberSaveable { mutableStateOf(journey?.title ?: "") }
    var startDate by rememberSaveable { mutableStateOf(journey?.startDate ?: LocalDate.now()) }
    var endDate by rememberSaveable { mutableStateOf(journey?.endDate ?: LocalDate.now()) }
    var shareJourney by rememberSaveable { mutableStateOf(journey?.shared ?: false) }
    val locations = remember {
        mutableStateListOf<JourneyLocation>().apply {
            if (journey != null)
                addAll(journey.locations)
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showAddPinDialog by remember { mutableStateOf(false) }
    var pinToRemove by remember { mutableStateOf<JourneyLocation?>(null) }
    var showAddCountryDialog by remember { mutableStateOf(false) }

    val visitedCountries = remember {
        mutableStateListOf<String>().apply {
            if (journey != null) {
                addAll(journey.visitedCountries)
            }
        }
    }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.loadWorldMap(context)
    }

    val allCountries = remember(viewModel.countries) {
        viewModel.countries.map { it.name ?: it.id }.distinct().sorted()
    }

    Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // journey title
            item {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Journey Name") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.LightGray.copy(alpha = 0.15f),
                        unfocusedContainerColor = Color.White,

                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.primary,

                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Color.Gray,

                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }

            // pick start and end date
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextField(
                        value = startDate.toString(),
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                        label = { Text("Start Date") },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    showDatePicker = true
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.DateRange,
                                    contentDescription = null
                                )
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.LightGray.copy(alpha = 0.15f),
                            unfocusedContainerColor = Color.White,

                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.primary,

                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = Color.Gray,

                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )

                    TextField(
                        value = endDate.toString(),
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                        label = { Text("End Date") },
                        trailingIcon = {
                            IconButton(
                                onClick = { showDatePicker = true }
                            ) {
                                Icon(
                                    Icons.Outlined.DateRange,
                                    contentDescription = null
                                )
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.LightGray.copy(alpha = 0.15f),
                            unfocusedContainerColor = Color.White,

                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.primary,

                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = Color.Gray,

                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            }

            // share journey with friends
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Automatically share with your friends",
                        modifier = Modifier.weight(1f)
                    )

                    Switch(
                        checked = shareJourney,
                        onCheckedChange = {
                            shareJourney = it
                        }
                    )
                }
            }

            // Visited Countries
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Visited Countries",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    if (visitedCountries.isEmpty()) {
                        Text(
                            text = "No countries selected yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    } else {
                        val scrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(scrollState),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            visitedCountries.forEach { country ->
                                Row(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = country,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove country",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { visitedCountries.remove(country) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(50),
                    onClick = {
                        showAddCountryDialog = true
                    }
                ) {
                    Icon(
                        Icons.Default.Add,
                        null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Select Visited Countries")
                }
            }

            // add pin button
            item {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(50),
                    onClick = {
                        showAddPinDialog = true
                    }
                ) {
                    Icon(
                        Icons.Outlined.AddLocationAlt,
                        null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text("Add Pin")
                }
            }

            // Pins
            items(locations) { location ->
                PinListItem(
                    location = location,
                    onRemove = {
                        pinToRemove = location
                    }
                )
            }

            // spacer so list is not behind button
            item {
                Spacer(Modifier.height(90.dp))
            }
        }

        // button for save and discard
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(50))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(50)
                )
        ) {
            // Discard
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.background)
                    .clickable(onClick = onDiscard),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Discard",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Save / Create
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        val result = Journey(
                            id = journey?.id ?: 0,
                            title = title,
                            startDate = startDate,
                            endDate = endDate,
                            shared = shareJourney,
                            locations = locations.toList(),
                            visitedCountries = visitedCountries.toList()
                        )
                        onSave(result)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isEditing) "Save" else "Create",
                    color = MaterialTheme.colorScheme.background,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // dialog to schow datepicker
        if (showDatePicker) {
            JourneyDateRangePicker(
                startDate = startDate,
                endDate = endDate,
                onDismiss = { showDatePicker = false },
                onConfirm = { start, end ->
                    startDate = start
                    endDate = end
                    showDatePicker = false
                }
            )
        }

        // dialog to add pin
        if (showAddPinDialog) {
            AddPinDialog(
                onDismiss = { showAddPinDialog = false },
                onAdd = {
                    locations.add(it)
                    showAddPinDialog = false
                }
            )
        }

        // dialog to remove pin
        pinToRemove?.let { location ->
            RemovePinDialog(
                location = location,
                onDismiss = { pinToRemove = null },
                onRemove = {
                    locations.remove(location)
                    pinToRemove = null
                }
            )
        }

        if (showAddCountryDialog) {
            AddCountryDialog(
                allCountries = allCountries,
                selectedCountries = visitedCountries,
                onDismiss = { showAddCountryDialog = false },
                onCountryToggle = { country ->
                    if (visitedCountries.contains(country)) {
                        visitedCountries.remove(country)
                    } else {
                        visitedCountries.add(country)
                    }
                }
            )
        }
    }
}

@Composable
fun AddCountryDialog(
    allCountries: List<String>,
    selectedCountries: List<String>,
    onDismiss: () -> Unit,
    onCountryToggle: (String) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    
    val filteredCountries = remember(searchQuery, allCountries) {
        allCountries.filter {
            it.contains(searchQuery, ignoreCase = true)
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Visited Countries") },
        text = {
            Column(modifier = Modifier.height(300.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Country") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    }
                )
                
                Spacer(Modifier.height(12.dp))
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredCountries) { country ->
                        val isSelected = selectedCountries.contains(country)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCountryToggle(country)
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = country,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun PinListItem (
    location: JourneyLocation,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.width(16.dp))

        Row (
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = location.name,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.width(23.dp))

            Column() {
                Text(
                    text = "Latitude: ${location.latitude}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = "Longitude: ${location.longitude}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        IconButton(
            onClick = onRemove
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    HorizontalDivider(
        color = Color.LightGray.copy(alpha = .3f),
        modifier = Modifier.padding(start = 50.dp)
    )
}

@Composable
fun JourneyDateRangePicker (
    startDate: LocalDate,
    endDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit
) {
    val pickerState = rememberDateRangePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = pickerState.selectedStartDateMillis?.let {
                        Instant.ofEpochMilli(it)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    val end = pickerState.selectedEndDateMillis?.let {
                        Instant.ofEpochMilli(it)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }

                    if (start != null && end != null) {
                        onConfirm(start, end)
                    }
                }
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) { Text("Cancel") }
        }
    ) {
        DateRangePicker(state = pickerState)
    }
}

@Composable
fun AddPinDialog (
    onDismiss: () -> Unit,
    onAdd: (JourneyLocation) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var latitude by rememberSaveable { mutableStateOf("") }
    var longitude by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Pin") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.LightGray.copy(alpha = 0.15f),
                        unfocusedContainerColor = Color.White,

                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(Modifier.height(12.dp))

                TextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Latitude") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.LightGray.copy(alpha = 0.15f),
                        unfocusedContainerColor = Color.White,

                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(Modifier.height(12.dp))

                TextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Longitude") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.LightGray.copy(alpha = 0.15f),
                        unfocusedContainerColor = Color.White,

                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latitude.toDoubleOrNull()
                    val lng = longitude.toDoubleOrNull()

                    if (lat != null && lng != null) {
                        onAdd(
                            JourneyLocation(
                                id = System.currentTimeMillis().toInt(),
                                name = name,
                                latitude = lat,
                                longitude = lng
                            )
                        )
                    }
                }
            ) { Text("Add") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RemovePinDialog (
    location: JourneyLocation,
    onDismiss: () -> Unit,
    onRemove: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Remove Pin") },
        text = { Text( "Are you sure you want to remove \"${location.name}\"?") },
        confirmButton = {
            Button(
                onClick = onRemove,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Remove") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}