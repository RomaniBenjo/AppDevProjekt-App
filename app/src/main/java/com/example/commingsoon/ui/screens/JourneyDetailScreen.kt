package com.example.commingsoon.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.commingsoon.viewmodels.JourneyLocation
import com.example.commingsoon.viewmodels.JourneyViewModel

@Composable
fun JourneyDetailScreen (
    journeyId: Int,
    viewModel: JourneyViewModel,
    onEdit: () -> Unit = {}
) {
    val journey = viewModel.getJourney(journeyId) ?: return

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Date
        Text(
            text = "${journey.startDate}  -  ${journey.endDate}",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(Modifier.height(16.dp))

        // Map Placeholder
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
                Text("Map Placeholder")

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
            onClick = onEdit
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null
            )

            Spacer(Modifier.width(8.dp))

            Text("Edit Journey")
        }
    }
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
                style = MaterialTheme.typography.titleMedium
            )
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "Lat: ${location.latitude}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Text(
                text = "Lng: ${location.longitude}",
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