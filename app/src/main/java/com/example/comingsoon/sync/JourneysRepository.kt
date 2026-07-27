package com.example.comingsoon.sync

import com.example.comingsoon.auth.AuthSessionStore
import com.example.comingsoon.db.JourneyDao
import com.example.comingsoon.db.JourneyEntity
import com.example.comingsoon.db.SharedJourneyDao
import com.example.comingsoon.db.SharedJourneyEntity
import com.example.comingsoon.viewmodels.JourneyLocation
import com.example.comingsoon.viewmodels.Journey
import java.time.LocalDate

data class JourneyShareSnapshot(
    val ownerId: Int,
    val recipientId: Int,
    val ownerName: String,
    val ownerEmail: String,
    val ownerPictureUrl: String?,
    val shareType: String,
    val sharedAt: String,
    val journey: Journey
)

class JourneysRepository(
    private val apiClient: JourneysApiClient,
    private val sessionStore: AuthSessionStore,
    private val journeyDao: JourneyDao,
    private val sharedJourneyDao: SharedJourneyDao
) {
    fun hasSession(): Boolean = sessionStore.load() != null
    fun currentUserId(): Int? = sessionStore.load()?.user?.id?.toInt()

    /** Push local dirty rows, then pull the authoritative list, then reconcile. */
    suspend fun synchronize() {
        val token = token()

        journeyDao.getUnsyncedJourneys().forEach { local ->
            try {
                when {
                    local.deletedLocally && local.serverId != null -> {
                        apiClient.deleteJourney(token, local.serverId)
                        journeyDao.deleteById(local.id)
                    }
                    local.deletedLocally -> journeyDao.deleteById(local.id)
                    local.serverId == null -> {
                        val created = apiClient.createJourney(token, local.toUpsertBody())
                        journeyDao.update(local.copy(serverId = created.id, pendingSync = false, isSynced = true))
                    }
                    else -> {
                        apiClient.updateJourney(token, local.serverId, local.toUpsertBody())
                        journeyDao.update(local.copy(pendingSync = false, isSynced = true))
                    }
                }
            } catch (e: JourneysApiException) {
                // Leave pendingSync = true; retried on the next sync cycle.
            }
        }

        val remote = apiClient.listJourneys(token)
        val remoteIds = remote.mapTo(mutableSetOf()) { it.id }
        val unlinkedLocal = journeyDao.getAllForSync()
            .filter { it.serverId == null && !it.pendingSync }
            .toMutableList()
        remote.forEach { server ->
            // A local row with no serverId can be pre-existing data that predates this
            // sync feature (e.g. the same seeded sample journeys on two devices). Match
            // it to the server row by exact content so it gets linked instead of
            // duplicated once another device pushes "the same" journey for the first time.
            val existing = journeyDao.getByServerId(server.id)
                ?: unlinkedLocal.find {
                    it.title == server.title &&
                        it.startDate.toString() == server.startDate &&
                        it.endDate.toString() == server.endDate
                }?.also { unlinkedLocal.remove(it) }
            journeyDao.insert(server.toEntity(localId = existing?.id ?: 0))
        }
        journeyDao.getAllForSync()
            .filter { it.serverId != null && it.serverId !in remoteIds && !it.pendingSync }
            .forEach { journeyDao.deleteById(it.id) }
    }

    suspend fun shareJourney(localJourneyId: Int, friendUserId: Int) {
        // A newly-created offline journey needs a server id before it can be shared.
        synchronize()
        val serverId = journeyDao.getJourneyById(localJourneyId)?.serverId
            ?: throw JourneysApiException("Die Reise konnte noch nicht synchronisiert werden.")
        apiClient.shareJourney(token(), serverId, friendUserId)
    }

    suspend fun unshareJourney(localJourneyId: Int, friendUserId: Int) {
        val serverId = journeyDao.getJourneyById(localJourneyId)?.serverId
            ?: throw JourneysApiException("Die Reise konnte noch nicht synchronisiert werden.")
        apiClient.unshareJourney(token(), serverId, friendUserId)
    }

    suspend fun createShareLink(localJourneyId: Int): ServerJourneyShareLink {
        synchronize()
        val serverId = journeyDao.getJourneyById(localJourneyId)?.serverId
            ?: throw JourneysApiException("Die Reise konnte noch nicht synchronisiert werden.")
        return apiClient.createShareLink(token(), serverId)
    }

    suspend fun acceptShareLink(shareToken: String): JourneyShareSnapshot {
        val share = apiClient.acceptShareLink(token(), shareToken)
        refreshJourneyShares()
        return share.toSnapshot()
    }

    suspend fun loadCachedJourneyShares(): List<JourneyShareSnapshot> {
        val viewerId = currentUserId() ?: return emptyList()
        return sharedJourneyDao.getForViewer(viewerId).map { it.toSnapshot() }
    }

    suspend fun refreshJourneyShares(): List<JourneyShareSnapshot> {
        val viewerId = currentUserId()
            ?: throw JourneysApiException("Deine Sitzung ist abgelaufen. Bitte melde dich erneut an.")
        val remote = apiClient.listJourneyShares(token())
        sharedJourneyDao.replaceForViewer(viewerId, remote.map { it.toEntity(viewerId) })
        return remote.map { it.toSnapshot() }
    }

    suspend fun listJourneyShares(): List<JourneyShareSnapshot> = refreshJourneyShares()

    private fun token(): String = sessionStore.load()?.accessToken
        ?: throw JourneysApiException("Deine Sitzung ist abgelaufen. Bitte melde dich erneut an.")

    private fun JourneyEntity.toUpsertBody(): JourneyUpsertBody = JourneyUpsertBody(
        title = title,
        startDate = startDate.toString(),
        endDate = endDate.toString(),
        shared = shared,
        locations = locations.map { ServerJourneyLocation(it.id, it.name, it.latitude, it.longitude) },
        visitedCountries = visitedCountries
    )

    private fun ServerJourney.toEntity(localId: Int): JourneyEntity = JourneyEntity(
        id = localId,
        title = title,
        startDate = LocalDate.parse(startDate),
        endDate = LocalDate.parse(endDate),
        shared = shared,
        locations = locations.map { JourneyLocation(it.id, it.name, it.latitude, it.longitude) },
        visitedCountries = visitedCountries,
        pendingSync = false,
        isSynced = true,
        serverId = id,
        deletedLocally = false
    )

    private fun ServerJourney.toSharedDomain(ownerId: Int): Journey =
        Journey(
            id = id,
            title = title,
            startDate = LocalDate.parse(startDate),
            endDate = LocalDate.parse(endDate),
            shared = shared,
            locations = locations.map { JourneyLocation(it.id, it.name, it.latitude, it.longitude) },
            visitedCountries = visitedCountries,
            serverId = id,
            ownerId = ownerId,
            isOwned = false
        )

    private fun ServerJourneyShare.toSnapshot() = JourneyShareSnapshot(
        ownerId = ownerId,
        recipientId = recipientId,
        ownerName = owner.name?.takeIf { it.isNotBlank() } ?: owner.email.substringBefore('@'),
        ownerEmail = owner.email,
        ownerPictureUrl = owner.pictureUrl,
        shareType = shareType,
        sharedAt = createdAt,
        journey = journey.toSharedDomain(ownerId)
    )

    private fun ServerJourneyShare.toEntity(viewerId: Int) = SharedJourneyEntity(
        viewerId = viewerId,
        ownerId = ownerId,
        recipientId = recipientId,
        serverJourneyId = journey.id,
        ownerName = owner.name?.takeIf { it.isNotBlank() } ?: owner.email.substringBefore('@'),
        ownerEmail = owner.email,
        ownerPictureUrl = owner.pictureUrl,
        shareType = shareType,
        sharedAt = createdAt,
        title = journey.title,
        startDate = LocalDate.parse(journey.startDate),
        endDate = LocalDate.parse(journey.endDate),
        shared = journey.shared,
        locations = journey.locations.map {
            JourneyLocation(it.id, it.name, it.latitude, it.longitude)
        },
        visitedCountries = journey.visitedCountries
    )

    private fun SharedJourneyEntity.toSnapshot() = JourneyShareSnapshot(
        ownerId = ownerId,
        recipientId = recipientId,
        ownerName = ownerName,
        ownerEmail = ownerEmail,
        ownerPictureUrl = ownerPictureUrl,
        shareType = shareType,
        sharedAt = sharedAt,
        journey = Journey(
            id = serverJourneyId,
            title = title,
            startDate = startDate,
            endDate = endDate,
            shared = shared,
            locations = locations,
            visitedCountries = visitedCountries,
            serverId = serverJourneyId,
            ownerId = ownerId,
            isOwned = false
        )
    )
}
