package com.example.comingsoon.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

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

