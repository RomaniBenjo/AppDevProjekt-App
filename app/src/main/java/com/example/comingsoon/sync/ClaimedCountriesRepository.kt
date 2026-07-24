package com.example.comingsoon.sync

import android.content.Context
import com.example.comingsoon.auth.AuthSessionStore
import com.example.comingsoon.db.ClaimedCountryDao
import com.example.comingsoon.db.ClaimedCountryEntity

class ClaimedCountriesRepository(
    private val apiClient: ClaimedCountriesApiClient,
    private val sessionStore: AuthSessionStore,
    private val claimedCountryDao: ClaimedCountryDao,
    context: Context
) {
    private val syncState = context.applicationContext
        .getSharedPreferences("claimed_countries_sync_state", Context.MODE_PRIVATE)

    fun hasSession(): Boolean = sessionStore.load() != null

    fun markClearAllPending() {
        syncState.edit().putBoolean(CLEAR_ALL_PENDING, true).apply()
    }

    suspend fun synchronize() {
        val token = token()

        if (syncState.getBoolean(CLEAR_ALL_PENDING, false)) {
            apiClient.clearAllClaims(token)
            syncState.edit().putBoolean(CLEAR_ALL_PENDING, false).apply()
        }

        claimedCountryDao.getUnsyncedClaims().forEach { local ->
            try {
                apiClient.claimCountry(token, local.id, local.name, local.claimedAt)
                claimedCountryDao.updateClaim(local.copy(pendingSync = false))
            } catch (e: ClaimedCountriesApiException) {
                // Leave pendingSync = true; retried on the next sync cycle.
            }
        }

        val remote = apiClient.listClaims(token)
        val existingIds = claimedCountryDao.getAllClaims().mapTo(mutableSetOf()) { it.id }
        remote.forEach { server ->
            if (server.countryId !in existingIds) {
                claimedCountryDao.insertClaim(
                    ClaimedCountryEntity(server.countryId, server.name, server.claimedAt, pendingSync = false)
                )
            }
        }
    }

    private fun token(): String = sessionStore.load()?.accessToken
        ?: throw ClaimedCountriesApiException("Deine Sitzung ist abgelaufen. Bitte melde dich erneut an.")

    private companion object {
        const val CLEAR_ALL_PENDING = "clear_all_pending"
    }
}
