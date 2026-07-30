package com.example.comingsoon.friends

import com.example.comingsoon.viewmodels.Journey
import com.example.comingsoon.viewmodels.JourneyLocation
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.math.abs

data class OfflineJourneyLocationPayload(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

data class OfflineJourneyPayload(
    val transferId: String,
    val sourceJourneyId: Int,
    val title: String,
    val startDate: String,
    val endDate: String,
    val locations: List<OfflineJourneyLocationPayload>,
    val visitedCountries: List<String>,
    val sharedAt: String
)

fun Journey.toOfflinePayload(
    transferId: String = UUID.randomUUID().toString(),
    sharedAt: String = Instant.now().toString()
): OfflineJourneyPayload = OfflineJourneyPayload(
    transferId = transferId,
    sourceJourneyId = id,
    title = title,
    startDate = startDate.toString(),
    endDate = endDate.toString(),
    locations = locations.map {
        OfflineJourneyLocationPayload(
            id = it.id,
            name = it.name,
            latitude = it.latitude,
            longitude = it.longitude
        )
    },
    visitedCountries = visitedCountries,
    sharedAt = sharedAt
)

fun OfflineJourneyPayload.toJourney(ownerId: Int): Journey = Journey(
    id = stableOfflineId("$ownerId:$transferId"),
    title = title,
    startDate = LocalDate.parse(startDate),
    endDate = LocalDate.parse(endDate),
    shared = false,
    locations = locations.map {
        JourneyLocation(
            id = it.id,
            name = it.name,
            latitude = it.latitude,
            longitude = it.longitude
        )
    },
    visitedCountries = visitedCountries,
    serverId = stableOfflineId(transferId),
    ownerId = ownerId,
    isOwned = false
)

internal fun stableOfflineId(value: String): Int {
    val hash = value.hashCode()
    return when (hash) {
        0 -> -1
        Int.MIN_VALUE -> Int.MIN_VALUE + 1
        else -> -abs(hash)
    }
}
