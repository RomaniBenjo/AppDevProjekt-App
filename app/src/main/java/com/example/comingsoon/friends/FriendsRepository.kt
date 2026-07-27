package com.example.comingsoon.friends

import android.content.Context
import com.example.comingsoon.auth.AuthSessionStore
import com.example.comingsoon.auth.AuthenticatedUser
import com.example.comingsoon.db.FriendDao
import com.example.comingsoon.db.FriendEntity
import java.security.MessageDigest
import java.util.UUID

data class StoredFriend(
    val identityKey: String,
    val serverUserId: Long?,
    val name: String,
    val email: String,
    val pictureUrl: String?,
    val addedNearby: Boolean,
    val isServerFriend: Boolean
)

data class FriendsSyncResult(
    val friends: List<StoredFriend>,
    val requests: ServerFriendRequests
)

data class OfflineFriendIdentity(
    val deviceId: String,
    val serverUserId: Long?,
    val name: String,
    val email: String,
    val pictureUrl: String?
) {
    val identityKey: String
        get() = serverUserId?.let { "server:$it" } ?: "device:$deviceId"
}

class FriendsRepository(
    private val apiClient: FriendsApiClient,
    private val sessionStore: AuthSessionStore,
    private val friendDao: FriendDao,
    context: Context
) {
    private val identityPreferences = context.applicationContext.getSharedPreferences(
        "offline_friend_identity",
        Context.MODE_PRIVATE
    )

    fun hasSession(): Boolean = sessionStore.load() != null
    fun currentUserId(): Int? = sessionStore.load()?.user?.id?.toInt()

    fun currentOfflineIdentity(): OfflineFriendIdentity {
        val user = sessionStore.cachedUser()
        return OfflineFriendIdentity(
            deviceId = localDeviceId(),
            serverUserId = user?.id,
            name = user?.displayName().orEmpty().ifBlank { "ComingSoon Nutzer" },
            email = user?.email.orEmpty(),
            pictureUrl = user?.pictureUrl
        )
    }

    suspend fun loadCachedFriends(): List<StoredFriend> =
        friendDao.getAll().mergeDuplicates().map { it.toStoredFriend() }

    /**
     * Reconciles cryptographically-random Nearby pairing IDs, deletion tombstones and the
     * authoritative server friend list.
     */
    suspend fun synchronizeFriends(): FriendsSyncResult {
        val accessToken = token()
        val ownUserId = requireNotNull(sessionStore.load()?.user?.id)
        val initialRemoteUserIds = apiClient.getFriends(accessToken)
            .mapTo(mutableSetOf(), AuthenticatedUser::id)
        friendDao.getAllForSync().forEach { local ->
            val peerUserId = local.serverUserId ?: return@forEach
            if (local.deletedLocally && !local.addedNearby) {
                if (peerUserId in initialRemoteUserIds) {
                    apiClient.removeFriend(accessToken, peerUserId.toInt())
                }
                friendDao.deleteByIdentityKey(local.identityKey)
                return@forEach
            }
            if (!local.addedNearby) return@forEach
            var synchronizedLocal = local
            val pairingId = local.pairingId ?: legacyPairingId(ownUserId, peerUserId).also {
                synchronizedLocal = local.copy(pairingId = it)
                friendDao.upsert(synchronizedLocal)
            }
            val result = apiClient.syncOfflinePairing(
                token = accessToken,
                pairingId = pairingId,
                peerUserId = peerUserId.toInt(),
                deleted = local.deletedLocally
            )
            when (result.status) {
                "active" -> friendDao.upsert(synchronizedLocal.copy(isServerFriend = true))
                "deleted" -> {
                    friendDao.deleteByIdentityKey(local.identityKey)
                }
                "pending" -> Unit
                else -> throw FriendsApiException(
                    "Der Server hat einen unbekannten Offline-Synchronisationsstatus geliefert."
                )
            }
        }

        val remoteFriends = apiClient.getFriends(accessToken)
        val requests = apiClient.getRequests(accessToken)
        val remoteUserIds = remoteFriends.mapTo(mutableSetOf(), AuthenticatedUser::id)
        friendDao.getAllForSync()
            .filter { it.isServerFriend && it.serverUserId !in remoteUserIds }
            .forEach { friendDao.deleteByIdentityKey(it.identityKey) }
        cacheServerFriends(remoteFriends)
        return FriendsSyncResult(
            friends = loadCachedFriends(),
            requests = requests
        )
    }

    suspend fun saveNearbyFriend(identity: OfflineFriendIdentity, pairingId: String) {
        val ownIdentity = currentOfflineIdentity()
        if (
            identity.deviceId == ownIdentity.deviceId ||
            identity.serverUserId != null && identity.serverUserId == ownIdentity.serverUserId
        ) {
            throw FriendsApiException("Du kannst dich nicht selbst als Freund hinzufügen.")
        }

        val existing = identity.serverUserId?.let { friendDao.getByServerUserId(it) }
            ?: friendDao.getByIdentityKey(identity.identityKey)
        val entity = FriendEntity(
            identityKey = existing?.identityKey ?: identity.identityKey,
            serverUserId = identity.serverUserId,
            deviceId = identity.deviceId,
            pairingId = pairingId,
            displayName = identity.name.trim().take(80).ifBlank { "ComingSoon Nutzer" },
            email = identity.email.trim().take(254),
            pictureUrl = identity.pictureUrl,
            addedNearby = true,
            isServerFriend = existing?.isServerFriend == true,
            deletedLocally = false,
            createdAtEpochMillis = existing?.createdAtEpochMillis ?: System.currentTimeMillis()
        )
        friendDao.upsert(entity)
    }

    suspend fun searchUsers(query: String) = apiClient.searchUsers(token(), query)
    suspend fun sendRequest(userId: Int) = apiClient.sendRequest(token(), userId)
    suspend fun acceptRequest(requestId: Int) = apiClient.acceptRequest(token(), requestId)
    suspend fun deleteRequest(requestId: Int) = apiClient.deleteRequest(token(), requestId)

    suspend fun markFriendDeleted(friend: StoredFriend) {
        val entity = friendDao.getByIdentityKey(friend.identityKey) ?: return
        if (entity.serverUserId == null) {
            friendDao.deleteByIdentityKey(entity.identityKey)
        } else {
            friendDao.upsert(entity.copy(deletedLocally = true))
        }
    }

    suspend fun listenForUpdates(onUpdate: (String) -> Unit) =
        apiClient.listenForFriendUpdates(token(), onUpdate)

    private suspend fun cacheServerFriends(users: List<AuthenticatedUser>) {
        friendDao.deleteServerOnlyFriends()
        users.forEach { user ->
            val existing = friendDao.getByServerUserId(user.id)
            friendDao.upsert(
                FriendEntity(
                    identityKey = existing?.identityKey ?: "server:${user.id}",
                    serverUserId = user.id,
                    deviceId = existing?.deviceId,
                    pairingId = existing?.pairingId,
                    displayName = user.displayName(),
                    email = user.email,
                    pictureUrl = user.pictureUrl,
                    addedNearby = existing?.addedNearby == true,
                    isServerFriend = true,
                    deletedLocally = existing?.deletedLocally == true,
                    createdAtEpochMillis = existing?.createdAtEpochMillis
                        ?: System.currentTimeMillis()
                )
            )
        }
    }

    private fun localDeviceId(): String {
        identityPreferences.getString(DEVICE_ID, null)?.let { return it }
        return UUID.randomUUID().toString().also {
            identityPreferences.edit().putString(DEVICE_ID, it).apply()
        }
    }

    private fun legacyPairingId(firstUserId: Long, secondUserId: Long): String {
        val canonical = listOf(firstUserId, secondUserId).sorted().joinToString("|")
        return MessageDigest.getInstance("SHA-256")
            .digest("legacy-offline-friend|$canonical".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun token(): String = sessionStore.load()?.accessToken
        ?: throw FriendsApiException("Deine Sitzung ist abgelaufen. Bitte melde dich erneut an.")

    private fun List<FriendEntity>.mergeDuplicates(): List<FriendEntity> =
        groupBy { it.serverUserId?.let { id -> "server:$id" } ?: it.identityKey }
            .values
            .map { entries ->
                entries.maxWith(
                    compareBy<FriendEntity> { it.addedNearby }
                        .thenBy { it.isServerFriend }
                        .thenBy { it.createdAtEpochMillis }
                )
            }

    private fun FriendEntity.toStoredFriend() = StoredFriend(
        identityKey = identityKey,
        serverUserId = serverUserId,
        name = displayName,
        email = email,
        pictureUrl = pictureUrl,
        addedNearby = addedNearby,
        isServerFriend = isServerFriend
    )

    private fun AuthenticatedUser.displayName(): String =
        name?.takeIf { it.isNotBlank() } ?: email.substringBefore('@')

    private companion object {
        const val DEVICE_ID = "device_id"
    }
}
