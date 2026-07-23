package com.example.comingsoon.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val identityKey: String,
    val serverUserId: Long?,
    val deviceId: String?,
    val pairingId: String?,
    val displayName: String,
    val email: String,
    val pictureUrl: String?,
    val addedNearby: Boolean,
    val isServerFriend: Boolean,
    val deletedLocally: Boolean,
    val createdAtEpochMillis: Long
)

@Dao
interface FriendDao {
    @Query(
        """
        SELECT * FROM friends
        WHERE deletedLocally = 0
        ORDER BY displayName COLLATE NOCASE ASC
        """
    )
    suspend fun getAll(): List<FriendEntity>

    @Query("SELECT * FROM friends")
    suspend fun getAllForSync(): List<FriendEntity>

    @Query("SELECT * FROM friends WHERE identityKey = :identityKey LIMIT 1")
    suspend fun getByIdentityKey(identityKey: String): FriendEntity?

    @Query("SELECT * FROM friends WHERE serverUserId = :serverUserId LIMIT 1")
    suspend fun getByServerUserId(serverUserId: Long): FriendEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(friend: FriendEntity)

    @Query("DELETE FROM friends WHERE isServerFriend = 1 AND addedNearby = 0")
    suspend fun deleteServerOnlyFriends()

    @Query("DELETE FROM friends WHERE identityKey = :identityKey")
    suspend fun deleteByIdentityKey(identityKey: String)

    @Query("DELETE FROM friends WHERE serverUserId = :serverUserId")
    suspend fun deleteByServerUserId(serverUserId: Long)
}
