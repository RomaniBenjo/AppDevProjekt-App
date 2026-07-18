package com.example.commingsoon.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalTime
import com.example.commingsoon.language.AppLanguage
import com.example.commingsoon.ui.theme.AppThemeType

class AppPreferenceRepository (context: Context) {
    private val dataStore = context.dataStore
    companion object {
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
        private val THEME = stringPreferencesKey("theme")
        private val LANGUAGE = stringPreferencesKey("language")
        private val NOTIFICATION_TIME = stringPreferencesKey("notification_time")
        private val JOURNEY_REMINDER_ENABLED = booleanPreferencesKey("journey_reminder_enabled")
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
                    journeyReminderEnabled = preferences[JOURNEY_REMINDER_ENABLED] ?: false
                )
            }

    suspend fun saveDarkMode(enabled: Boolean) {
        dataStore.edit { it[DARK_MODE] = enabled }
    }

    suspend fun saveTheme(theme: AppThemeType) {
        dataStore.edit { it[THEME] = theme.name }
    }

    suspend fun saveLanguage(language: AppLanguage) {
        dataStore.edit { it[LANGUAGE] = language.name }
    }

    suspend fun saveNotificationTime(time: LocalTime) {
        dataStore.edit { it[NOTIFICATION_TIME] = time.toString() }
    }

    suspend fun saveJourneyReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[JOURNEY_REMINDER_ENABLED] = enabled }
    }

}