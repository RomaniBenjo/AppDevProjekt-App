package com.example.comingsoon.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "claimed_countries")
data class ClaimedCountryEntity(
    @PrimaryKey val id: String, // Country ID / code (e.g. "us", "de" in lowercase to match SVG ids)
    val name: String,
    val claimedAt: Long
)
