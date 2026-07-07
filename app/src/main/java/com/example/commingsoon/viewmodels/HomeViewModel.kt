package com.example.commingsoon.viewmodels

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import java.time.LocalDate

data class Journey(
    val id: Long,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val pinCount: Int
)

class HomeViewModel : ViewModel() {

    // TODO: Platzhalter Reisen (nach Startzeitpunkt sotiert)

    val journeys = mutableStateListOf(
        Journey(
            id = 1,
            title = "Japan",
            startDate = LocalDate.of(2026, 8, 13),
            endDate = LocalDate.of(2026, 11, 10),
            pinCount = 3
        ),
        Journey(
            id = 2,
            title = "Iceland",
            startDate = LocalDate.of(2027, 3, 13),
            endDate = LocalDate.of(2027, 3, 25),
            pinCount = 5
        ),
        Journey(
            id = 3,
            title = "Sweden",
            startDate = LocalDate.of(2027, 4, 2),
            endDate = LocalDate.of(2027, 4, 8),
            pinCount = 5
        ),
        Journey(
            id = 4,
            title = "Canada",
            startDate = LocalDate.of(2027, 5, 2),
            endDate = LocalDate.of(2027, 7, 30),
            pinCount = 23
        ),
        Journey(
            id = 5,
            title = "Netherlands",
            startDate = LocalDate.of(2027, 10, 16),
            endDate = LocalDate.of(2027, 10, 20),
            pinCount = 12
        ),
        Journey(
            id = 6,
            title = "China",
            startDate = LocalDate.of(2027, 11, 16),
            endDate = LocalDate.of(2027, 12, 20),
            pinCount = 4
        ),
        Journey(
            id = 7,
            title = "Australia",
            startDate = LocalDate.of(2028, 2, 16),
            endDate = LocalDate.of(2028, 2, 20),
            pinCount = 12
        ),
        Journey(
            id = 8,
            title = "Netherlands",
            startDate = LocalDate.of(2028, 5, 16),
            endDate = LocalDate.of(2028, 5, 20),
            pinCount = 12
        )
    )
}