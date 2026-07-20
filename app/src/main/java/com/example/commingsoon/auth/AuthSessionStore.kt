package com.example.commingsoon.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class AuthSession(
    val accessToken: String,
    val expiresAtEpochSeconds: Long,
    val user: AuthenticatedUser
) {
    fun isValid(nowEpochSeconds: Long = System.currentTimeMillis() / 1_000): Boolean =
        accessToken.isNotBlank() && expiresAtEpochSeconds > nowEpochSeconds
}

class AuthSessionStore(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun save(response: ServerAuthResponse) {
        val now = System.currentTimeMillis() / 1_000
        preferences.edit()
            .putString(ACCESS_TOKEN, response.accessToken)
            .putLong(EXPIRES_AT, now + response.expiresIn)
            .putLong(USER_ID, response.user.id)
            .putString(USER_EMAIL, response.user.email)
            .putString(USER_NAME, response.user.name)
            .putString(USER_PICTURE_URL, response.user.pictureUrl)
            .apply()
    }

    fun load(): AuthSession? {
        val token = preferences.getString(ACCESS_TOKEN, null) ?: return null
        val email = preferences.getString(USER_EMAIL, null) ?: return null
        val session = AuthSession(
            accessToken = token,
            expiresAtEpochSeconds = preferences.getLong(EXPIRES_AT, 0),
            user = AuthenticatedUser(
                id = preferences.getLong(USER_ID, 0),
                email = email,
                name = preferences.getString(USER_NAME, null),
                pictureUrl = preferences.getString(USER_PICTURE_URL, null)
            )
        )
        return session.takeIf { it.isValid() } ?: run {
            clear()
            null
        }
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val FILE_NAME = "auth_session"
        const val ACCESS_TOKEN = "access_token"
        const val EXPIRES_AT = "expires_at"
        const val USER_ID = "user_id"
        const val USER_EMAIL = "user_email"
        const val USER_NAME = "user_name"
        const val USER_PICTURE_URL = "user_picture_url"
    }
}
