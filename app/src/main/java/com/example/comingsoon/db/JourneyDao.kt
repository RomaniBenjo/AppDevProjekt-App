package com.example.comingsoon.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction

@Dao
interface JourneyDao {
    @Query("SELECT * FROM journeys WHERE deletedLocally = 0 ORDER BY startDate DESC")
    suspend fun getAllJourneys(): List<JourneyEntity>

    @Query("SELECT * FROM journeys ORDER BY startDate DESC")
    suspend fun getAllForSync(): List<JourneyEntity>

    @Query("SELECT * FROM journeys WHERE id = :id")
    suspend fun getJourneyById(id: Int): JourneyEntity?

    @Query("SELECT * FROM journeys WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): JourneyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(journey: JourneyEntity): Long

    @Update
    suspend fun update(journey: JourneyEntity)

    @Delete
    suspend fun delete(journey: JourneyEntity)

    @Query("DELETE FROM journeys WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM journeys WHERE pendingSync = 1")
    suspend fun getUnsyncedJourneys(): List<JourneyEntity>
}

@Dao
interface SharedJourneyDao {
    @Query(
        """
        SELECT * FROM shared_journeys
        WHERE viewerId = :viewerId
        ORDER BY startDate DESC, serverJourneyId DESC
        """
    )
    suspend fun getForViewer(viewerId: Int): List<SharedJourneyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shares: List<SharedJourneyEntity>)

    @Query(
        """
        DELETE FROM shared_journeys
        WHERE viewerId = :viewerId
          AND shareType != 'offline'
        """
    )
    suspend fun deleteOnlineForViewer(viewerId: Int)

    @Transaction
    suspend fun replaceOnlineForViewer(viewerId: Int, shares: List<SharedJourneyEntity>) {
        deleteOnlineForViewer(viewerId)
        if (shares.isNotEmpty()) insertAll(shares)
    }
}

@Dao
interface PendingJourneyShareDao {
    @Query(
        """
        SELECT * FROM pending_journey_shares
        WHERE ownerId = :ownerId
        ORDER BY createdAtEpochMillis ASC
        """
    )
    suspend fun getForOwner(ownerId: Int): List<PendingJourneyShareEntity>

    @Query(
        """
        SELECT * FROM pending_journey_shares
        WHERE ownerId = :ownerId
          AND localJourneyId = :localJourneyId
          AND recipientId = :recipientId
        LIMIT 1
        """
    )
    suspend fun get(
        ownerId: Int,
        localJourneyId: Int,
        recipientId: Int
    ): PendingJourneyShareEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(action: PendingJourneyShareEntity)

    @Delete
    suspend fun delete(action: PendingJourneyShareEntity)

    @Query(
        """
        DELETE FROM pending_journey_shares
        WHERE ownerId = :ownerId
          AND localJourneyId = :localJourneyId
          AND recipientId = :recipientId
        """
    )
    suspend fun delete(
        ownerId: Int,
        localJourneyId: Int,
        recipientId: Int
    )
}

@Dao
interface ClaimedCountryDao {
    @Query("SELECT * FROM claimed_countries")
    suspend fun getAllClaims(): List<ClaimedCountryEntity>

    @Query("SELECT * FROM claimed_countries WHERE pendingSync = 1")
    suspend fun getUnsyncedClaims(): List<ClaimedCountryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClaim(claim: ClaimedCountryEntity): Long

    @Update
    suspend fun updateClaim(claim: ClaimedCountryEntity)

    @Query("DELETE FROM claimed_countries WHERE id = :id")
    suspend fun deleteClaimById(id: String)

    @Query("DELETE FROM claimed_countries")
    suspend fun deleteAllClaims()
}
