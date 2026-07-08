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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.commingsoon.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.commingsoon.viewmodels.JourneyViewModel
import com.example.commingsoon.viewmodels.Journey

@Composable
fun HomeScreen ( viewModel: JourneyViewModel = viewModel() ) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // TODO: world map
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(.33f)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
            ) {
                Text(
                    "placeholder for world map",
                    Modifier.align(Alignment.CenterHorizontally).padding(top = 20.dp)
                )
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
                    onClick = { /* TODO: nav to specific journey */ }
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
            onClick = { /* TODO: nav to new journey */ }
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