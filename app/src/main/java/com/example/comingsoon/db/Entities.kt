package com.example.comingsoon.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.comingsoon.viewmodels.Journey
import com.example.comingsoon.viewmodels.JourneyLocation
import java.time.LocalDate

@Entity(tableName = "journeys")
data class JourneyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val shared: Boolean? = null,
    val locations: List<JourneyLocation>,
    val visitedCountries: List<String>,
    val pendingSync: Boolean = false,
    val isSynced: Boolean = false
) {
    fun toDomain(): Journey {
        return Journey(
            id = id,
            title = title,
            startDate = startDate,
            endDate = endDate,
            shared = shared,
            locations = locations,
            visitedCountries = visitedCountries
        )
    }

    companion object {
        fun fromDomain(journey: Journey, pendingSync: Boolean = false, isSynced: Boolean = false): JourneyEntity {
            return JourneyEntity(
                id = journey.id,
                title = journey.title,
                startDate = journey.startDate,
                endDate = journey.endDate,
                shared = journey.shared,
                locations = journey.locations,
                visitedCountries = journey.visitedCountries,
                pendingSync = pendingSync,
                isSynced = isSynced
            )
        }
    }
}
