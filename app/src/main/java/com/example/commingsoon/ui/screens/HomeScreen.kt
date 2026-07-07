package com.example.commingsoon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.commingsoon.viewmodels.HomeViewModel
import com.example.commingsoon.viewmodels.Journey
import org.w3c.dom.Text

@Composable
fun HomeScreen ( viewModel: HomeViewModel = viewModel() ) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // TODO: world map
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(.33f)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("placeholder for world map")
            }
        }

        // list of journeys
        LazyColumn(
            modifier = Modifier.weight(.67f)
        ) {
            items(viewModel.journeys) {
                JourneyCard(
                    journey = it,
                    onClick = { /* TODO: nav to specific journey */ }
                )
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
                onClick = { /* TODO: nav to new journey */ }
            ) {
                Text("New Journey")
            }
        }
    }
}

@Composable
fun Timeline () {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    CircleShape
                )
        )

        Box(
            modifier = Modifier
                .width(3.dp)
                .height(70.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = .35f)
                )
        )
    }
}

@Composable
fun JourneyCard (
    journey: Journey,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dates
            Column (
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = journey.startDate.toString(),
                    color = Color.Gray.copy(alpha = .7f)
                )
                Text(
                    text = "-",
                    color = Color.Gray.copy(alpha = .7f)
                )
                Text(
                    text = journey.endDate.toString(),
                    color = Color.Gray.copy(alpha = .7f)
                )
            }

            Spacer(Modifier.width(24.dp))

            // Journey Name
            Text(
                text = journey.title,
                modifier = Modifier.weight(1f)
            )

        }

        HorizontalDivider(
            color = Color.LightGray.copy(alpha = .3f)
        )
    }
}