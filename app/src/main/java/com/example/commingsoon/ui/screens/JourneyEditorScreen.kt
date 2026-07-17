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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocationOn
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
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
import com.example.commingsoon.R
import com.example.commingsoon.language.appString
import com.example.commingsoon.viewmodels.Journey
import com.example.commingsoon.viewmodels.JourneyLocation
import com.example.commingsoon.viewmodels.JourneyViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
                    label = { Text(appString(R.string.journey_name)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.LightGray.copy(alpha = 0.15f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.15f),

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
                        label = { Text(appString(R.string.start_date)) },
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
                            unfocusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.15f),

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
                        label = { Text(appString(R.string.end_date)) },
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
                            unfocusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.15f),

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
                        text = appString(R.string.automatically_share),
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

                    Text(appString(R.string.new_pin))
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
                    text = appString(R.string.discard),
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
                            locations = locations.toList()
                        )
                        onSave(result)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isEditing) appString(R.string.save) else appString(R.string.create),
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
    }
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
            ) { Text(appString(R.string.ok)) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) { Text(appString(R.string.cancel)) }
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
        title = { Text(appString(R.string.new_pin)) },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(appString(R.string.journey_name)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.LightGray.copy(alpha = 0.15f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.15f),

                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(Modifier.height(12.dp))

                TextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(appString(R.string.latitude_name)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.LightGray.copy(alpha = 0.15f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.15f),

                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(Modifier.height(12.dp))

                TextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(appString(R.string.longitude_name)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.LightGray.copy(alpha = 0.15f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.15f),

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
            ) { Text(appString(R.string.add)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(appString(R.string.cancel)) }
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
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(appString(R.string.remove_pin)) },
        text = { Text(appString(R.string.remove_pin_dialog, location.name)) },
        confirmButton = {
            Button(
                onClick = onRemove,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) { Text(appString(R.string.remove)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(appString(R.string.cancel)) }
        }
    )
}