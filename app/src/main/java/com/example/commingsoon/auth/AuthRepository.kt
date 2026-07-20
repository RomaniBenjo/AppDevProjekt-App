package com.example.commingsoon.auth

class AuthRepository(
    private val apiClient: AuthApiClient,
    private val sessionStore: AuthSessionStore
) {
    suspend fun authenticateGoogleUser(googleIdToken: String): AuthSession {
        val response = apiClient.authenticateWithGoogle(googleIdToken)
        sessionStore.save(response)
        return checkNotNull(sessionStore.load()) {
            "Die Server-Sitzung konnte nicht gespeichert werden."
        }
    }

    fun currentSession(): AuthSession? = sessionStore.load()

    fun signOut() = sessionStore.clear()
}
