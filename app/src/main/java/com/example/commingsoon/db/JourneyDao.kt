package com.example.commingsoon.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface JourneyDao {
    @Query("SELECT * FROM journeys ORDER BY startDate DESC")
    suspend fun getAllJourneys(): List<JourneyEntity>

    @Query("SELECT * FROM journeys WHERE id = :id")
    suspend fun getJourneyById(id: Int): JourneyEntity?

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
