package com.example.comingsoon.sync

import com.example.comingsoon.auth.AuthSessionStore
import com.example.comingsoon.db.JourneyDao
import com.example.comingsoon.db.JourneyEntity
import com.example.comingsoon.viewmodels.JourneyLocation
import java.time.LocalDate

class JourneysRepository(
    private val apiClient: JourneysApiClient,
    private val sessionStore: AuthSessionStore,
    private val journeyDao: JourneyDao
) {
    fun hasSession(): Boolean = sessionStore.load() != null

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
}
