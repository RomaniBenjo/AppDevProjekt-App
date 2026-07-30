package com.example.comingsoon.sync

import com.example.comingsoon.auth.AuthSessionStore
import com.example.comingsoon.auth.AuthenticatedUser
import com.example.comingsoon.db.JourneyDao
import com.example.comingsoon.db.JourneyEntity
import com.example.comingsoon.db.PendingJourneyShareDao
import com.example.comingsoon.db.PendingJourneyShareEntity
import com.example.comingsoon.db.SharedJourneyDao
import com.example.comingsoon.db.SharedJourneyEntity
import com.example.comingsoon.friends.OfflineFriendIdentity
import com.example.comingsoon.friends.OfflineJourneyPayload
import com.example.comingsoon.friends.stableOfflineId
import com.example.comingsoon.friends.toJourney
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
    val journey: Journey,
    val localJourneyId: Int? = null
)

enum class PendingJourneyShareAction {
    SHARE,
    UNSHARE
}

internal fun effectiveJourneyShareType(
    remoteShareType: String?,
    pendingAction: PendingJourneyShareAction?
): String? = when (pendingAction) {
    PendingJourneyShareAction.SHARE -> "manual"
    PendingJourneyShareAction.UNSHARE -> null
    null -> remoteShareType
}

data class PendingJourneyShare(
    val ownerId: Int,
    val localJourneyId: Int,
    val recipientId: Int,
    val action: PendingJourneyShareAction,
    val createdAtEpochMillis: Long
)

class JourneysRepository(
    private val apiClient: JourneysApiClient,
    private val sessionStore: AuthSessionStore,
    private val journeyDao: JourneyDao,
    private val sharedJourneyDao: SharedJourneyDao,
    private val pendingJourneyShareDao: PendingJourneyShareDao
) {
    fun hasSession(): Boolean = sessionStore.load() != null
    fun currentUserId(): Int? = sessionStore.cachedUser()?.id?.toInt()
    fun currentUser(): AuthenticatedUser? = sessionStore.cachedUser()

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

        synchronizePendingShares(token)

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

    suspend fun queueShare(localJourneyId: Int, friendUserId: Int) {
        queueShareAction(localJourneyId, friendUserId, PendingJourneyShareAction.SHARE)
    }

    suspend fun queueUnshare(localJourneyId: Int, friendUserId: Int) {
        queueShareAction(localJourneyId, friendUserId, PendingJourneyShareAction.UNSHARE)
    }

    suspend fun loadPendingShares(): List<PendingJourneyShare> {
        val ownerId = currentUserId() ?: return emptyList()
        return pendingJourneyShareDao.getForOwner(ownerId).mapNotNull { it.toDomainOrNull() }
    }

    suspend fun shareJourney(localJourneyId: Int, friendUserId: Int) {
        queueShare(localJourneyId, friendUserId)
        synchronize()
    }

    suspend fun unshareJourney(localJourneyId: Int, friendUserId: Int) {
        queueUnshare(localJourneyId, friendUserId)
        synchronize()
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
        sharedJourneyDao.replaceOnlineForViewer(viewerId, remote.map { it.toEntity(viewerId) })
        return loadCachedJourneyShares()
    }

    suspend fun storeReceivedOfflineShare(
        owner: OfflineFriendIdentity,
        payload: OfflineJourneyPayload
    ): JourneyShareSnapshot {
        val viewerId = currentUserId()
            ?: throw JourneysApiException("Bitte melde dich an, um eine Offline-Reise zu empfangen.")
        val ownerId = owner.serverUserId
            ?.takeIf { it in 1..Int.MAX_VALUE.toLong() }
            ?.toInt()
            ?: stableOfflineId(owner.identityKey)
        val journey = payload.toJourney(ownerId)
        val entity = SharedJourneyEntity(
            viewerId = viewerId,
            ownerId = ownerId,
            recipientId = viewerId,
            serverJourneyId = requireNotNull(journey.serverId),
            localJourneyId = null,
            ownerName = owner.name,
            ownerEmail = owner.email,
            ownerPictureUrl = owner.pictureUrl,
            shareType = OFFLINE_SHARE_TYPE,
            sharedAt = payload.sharedAt,
            title = journey.title,
            startDate = journey.startDate,
            endDate = journey.endDate,
            shared = false,
            locations = journey.locations,
            visitedCountries = journey.visitedCountries
        )
        sharedJourneyDao.insertAll(listOf(entity))
        return entity.toSnapshot()
    }

    suspend fun storeSentOfflineShare(
        recipient: OfflineFriendIdentity,
        payload: OfflineJourneyPayload
    ): JourneyShareSnapshot {
        val owner = currentUser()
            ?: throw JourneysApiException("Bitte melde dich an, um eine Offline-Reise zu teilen.")
        val viewerId = owner.id.toInt()
        val recipientId = recipient.serverUserId
            ?.takeIf { it in 1..Int.MAX_VALUE.toLong() }
            ?.toInt()
            ?: stableOfflineId(recipient.identityKey)
        val localJourney = journeyDao.getJourneyById(payload.sourceJourneyId)
            ?: throw JourneysApiException("Die Reise wurde nicht gefunden.")
        val offlineJourneyId = stableOfflineId(payload.transferId)
        val entity = SharedJourneyEntity(
            viewerId = viewerId,
            ownerId = viewerId,
            recipientId = recipientId,
            serverJourneyId = offlineJourneyId,
            localJourneyId = localJourney.id,
            ownerName = owner.displayName(),
            ownerEmail = owner.email,
            ownerPictureUrl = owner.pictureUrl,
            shareType = OFFLINE_SHARE_TYPE,
            sharedAt = payload.sharedAt,
            title = localJourney.title,
            startDate = localJourney.startDate,
            endDate = localJourney.endDate,
            shared = false,
            locations = localJourney.locations,
            visitedCountries = localJourney.visitedCountries
        )
        sharedJourneyDao.insertAll(listOf(entity))
        return entity.toSnapshot().copy(localJourneyId = localJourney.id)
    }

    suspend fun listJourneyShares(): List<JourneyShareSnapshot> = refreshJourneyShares()

    private fun token(): String = sessionStore.load()?.accessToken
        ?: throw JourneysApiException("Deine Sitzung ist abgelaufen. Bitte melde dich erneut an.")

    private suspend fun queueShareAction(
        localJourneyId: Int,
        friendUserId: Int,
        action: PendingJourneyShareAction
    ) {
        require(friendUserId > 0) {
            "Diese Freundschaft muss zuerst mit einem Server-Konto verknüpft werden."
        }
        val ownerId = currentUserId()
            ?: throw JourneysApiException("Bitte melde dich an, um eine Reise zu teilen.")
        val journey = journeyDao.getJourneyById(localJourneyId)
            ?: throw JourneysApiException("Die Reise wurde nicht gefunden.")
        check(!journey.deletedLocally) { "Gelöschte Reisen können nicht geteilt werden." }

        pendingJourneyShareDao.upsert(
            PendingJourneyShareEntity(
                ownerId = ownerId,
                localJourneyId = localJourneyId,
                recipientId = friendUserId,
                action = action.name,
                createdAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    private suspend fun synchronizePendingShares(token: String) {
        val ownerId = currentUserId() ?: return
        pendingJourneyShareDao.getForOwner(ownerId).forEach { pending ->
            val journey = journeyDao.getJourneyById(pending.localJourneyId)
            val serverId = journey?.serverId
            if (journey == null || journey.deletedLocally) {
                pendingJourneyShareDao.delete(pending)
                return@forEach
            }
            if (serverId == null) return@forEach

            try {
                when (pending.toDomainOrNull()?.action) {
                    PendingJourneyShareAction.SHARE -> {
                        apiClient.shareJourney(token, serverId, pending.recipientId)
                    }
                    PendingJourneyShareAction.UNSHARE -> {
                        apiClient.unshareJourney(token, serverId, pending.recipientId)
                    }
                    null -> Unit
                }
                pendingJourneyShareDao.delete(pending)
            } catch (exception: JourneysApiException) {
                // DELETE is idempotent from the outbox's perspective: an already absent
                // manual share has reached the requested final state.
                if (
                    pending.action == PendingJourneyShareAction.UNSHARE.name &&
                    exception.statusCode == 404
                ) {
                    pendingJourneyShareDao.delete(pending)
                }
                // Other failures remain queued and are retried by the next sync.
            }
        }
    }

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
        localJourneyId = null,
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
        ),
        localJourneyId = localJourneyId
    )

    private fun PendingJourneyShareEntity.toDomainOrNull(): PendingJourneyShare? {
        val parsedAction = runCatching {
            PendingJourneyShareAction.valueOf(action)
        }.getOrNull() ?: return null
        return PendingJourneyShare(
            ownerId = ownerId,
            localJourneyId = localJourneyId,
            recipientId = recipientId,
            action = parsedAction,
            createdAtEpochMillis = createdAtEpochMillis
        )
    }

    private fun AuthenticatedUser.displayName(): String =
        name?.takeIf { it.isNotBlank() } ?: email.substringBefore('@')

    private companion object {
        const val OFFLINE_SHARE_TYPE = "offline"
    }
}
