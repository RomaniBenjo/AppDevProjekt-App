package com.example.comingsoon.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalTime
import com.example.comingsoon.language.AppLanguage
import com.example.comingsoon.language.persistAppLanguage
import com.example.comingsoon.ui.theme.AppThemeType

class AppPreferenceRepository (private val context: Context) {
    private val dataStore = context.dataStore
    companion object {
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
        private val THEME = stringPreferencesKey("theme")
        private val LANGUAGE = stringPreferencesKey("language")
        private val NOTIFICATION_TIME = stringPreferencesKey("notification_time")
        private val JOURNEY_REMINDER_ENABLED = booleanPreferencesKey("journey_reminder_enabled")
        private val LIVE_LOCATION_SHARING_ENABLED = booleanPreferencesKey("live_location_sharing_enabled")
    }

    val settingsFlow: Flow<AppSettings> =
        dataStore.data.catch {
                if (it is IOException) {
                    emit(emptyPreferences())
                }
                else {
                    throw it
                }
            }

            .map { preferences ->
                AppSettings(
                    darkMode = preferences[DARK_MODE] ?: false,
                    theme = AppThemeType.valueOf(preferences[THEME] ?: AppThemeType.PINK.name),
                    language = AppLanguage.valueOf(preferences[LANGUAGE] ?: AppLanguage.ENGLISH.name),
                    notificationTime = LocalTime.parse(preferences[NOTIFICATION_TIME] ?: "09:00"),
                    journeyReminderEnabled = preferences[JOURNEY_REMINDER_ENABLED] ?: false,
                    liveLocationSharingEnabled = preferences[LIVE_LOCATION_SHARING_ENABLED] ?: false
                )
            }

    suspend fun saveDarkMode(enabled: Boolean) {
        dataStore.edit { it[DARK_MODE] = enabled }
    }

    suspend fun saveTheme(theme: AppThemeType) {
        dataStore.edit { it[THEME] = theme.name }
    }

    suspend fun saveLanguage(language: AppLanguage) {
        context.persistAppLanguage(language)
        dataStore.edit { it[LANGUAGE] = language.name }
    }

    fun cacheLanguage(language: AppLanguage) {
        context.persistAppLanguage(language)
    }

    suspend fun saveNotificationTime(time: LocalTime) {
        dataStore.edit { it[NOTIFICATION_TIME] = time.toString() }
    }

    suspend fun saveJourneyReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[JOURNEY_REMINDER_ENABLED] = enabled }
    }

    suspend fun saveLiveLocationSharingEnabled(enabled: Boolean) {
        dataStore.edit { it[LIVE_LOCATION_SHARING_ENABLED] = enabled }
    }

    suspend fun isLiveLocationSharingEnabled(): Boolean =
        settingsFlow.first().liveLocationSharingEnabled

}
