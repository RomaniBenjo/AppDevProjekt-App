package com.example.comingsoon.db

import androidx.room.Entity
import androidx.room.Index
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
    val isSynced: Boolean = false,
    val serverId: Int? = null,
    val deletedLocally: Boolean = false
) {
    fun toDomain(): Journey {
        return Journey(
            id = id,
            title = title,
            startDate = startDate,
            endDate = endDate,
            shared = shared,
            locations = locations,
            visitedCountries = visitedCountries,
            serverId = serverId
        )
    }

    companion object {
        fun fromDomain(
            journey: Journey,
            pendingSync: Boolean = false,
            isSynced: Boolean = false,
            serverId: Int? = null,
            deletedLocally: Boolean = false
        ): JourneyEntity {
            return JourneyEntity(
                id = journey.id,
                title = journey.title,
                startDate = journey.startDate,
                endDate = journey.endDate,
                shared = journey.shared,
                locations = journey.locations,
                visitedCountries = journey.visitedCountries,
                pendingSync = pendingSync,
                isSynced = isSynced,
                serverId = serverId,
                deletedLocally = deletedLocally
            )
        }
    }
}

@Entity(
    tableName = "shared_journeys",
    primaryKeys = ["viewerId", "ownerId", "recipientId", "serverJourneyId"],
    indices = [
        Index(value = ["viewerId"]),
        Index(value = ["ownerId", "serverJourneyId"])
    ]
)
data class SharedJourneyEntity(
    val viewerId: Int,
    val ownerId: Int,
    val recipientId: Int,
    val serverJourneyId: Int,
    val localJourneyId: Int?,
    val ownerName: String,
    val ownerEmail: String,
    val ownerPictureUrl: String?,
    val shareType: String,
    val sharedAt: String,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val shared: Boolean?,
    val locations: List<JourneyLocation>,
    val visitedCountries: List<String>
)

@Entity(
    tableName = "pending_journey_shares",
    primaryKeys = ["ownerId", "localJourneyId", "recipientId"],
    indices = [
        Index(value = ["ownerId"]),
        Index(value = ["localJourneyId"])
    ]
)
data class PendingJourneyShareEntity(
    val ownerId: Int,
    val localJourneyId: Int,
    val recipientId: Int,
    val action: String,
    val createdAtEpochMillis: Long
)
