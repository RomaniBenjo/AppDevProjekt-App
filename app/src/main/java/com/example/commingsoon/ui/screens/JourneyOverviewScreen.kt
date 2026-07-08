package com.example.commingsoon.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.commingsoon.viewmodels.Journey
import com.example.commingsoon.viewmodels.JourneyViewModel
import com.example.commingsoon.R

@Composable
fun JourneyOverviewScreen (
    viewModel: JourneyViewModel
) {
    var mapExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    var expandedJourneyId by rememberSaveable {
        mutableStateOf<Int?>(null)
    }

    // Map
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Map Placeholder
        AnimatedContent(
            targetState = mapExpanded,
            label = ""
        ) { expanded ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (expanded) 280.dp else 80.dp)
                    .padding(16.dp)
                    .clickable {
                        mapExpanded = !mapExpanded
                    }
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        if (expanded)
                            "World Map Placeholder"
                        else
                            "Tap to expand map"
                    )
                }
            }
        }

        // Journeys List
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

                JourneyCard(
                    journey = journey,
                    isFirst = isFirst,
                    isLast = isLast,
                    onClick = onClick
                )

                AnimatedVisibility(
                    visible = isExpanded
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 70.dp,
                                end = 16.dp,
                                bottom = 12.dp
                            ),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {

                        TextButton(
                            onClick = onChange
                        ) {
                            Text("Change")
                        }

                        TextButton(
                            onClick = onRemove
                        ) {
                            Text("Remove")
                        }

                        TextButton(
                            onClick = onShare
                        ) {
                            Text("Share")
                        }
                    }
                }
            }
        }

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
                    onChange = { },
                    onRemove = { },
                    onShare = { }
                )
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
            onClick = {
                // TODO
            }
        ) {
            Text(stringResource(R.string.new_journey))
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
        JourneyCard(
            journey = journey,
            isFirst = isFirst,
            isLast = isLast,
            onClick = onClick
        )
        AnimatedVisibility(
            visible = isExpanded
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 70.dp,
                        end = 16.dp,
                        bottom = 12.dp
                    ),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(
                    onClick = onChange
                ) {
                    Text("Change")
                }
                TextButton(
                    onClick = onRemove
                ) {
                    Text("Remove")
                }
                TextButton(
                    onClick = onShare
                ) {
                    Text("Share")
                }
            }
        }
    }
}