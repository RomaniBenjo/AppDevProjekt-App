package com.example.comingsoon.data

import com.example.comingsoon.language.AppLanguage
import com.example.comingsoon.ui.theme.AppThemeType
import java.time.LocalTime

data class AppSettings(
    val darkMode: Boolean = false,
    val theme: AppThemeType = AppThemeType.PINK,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val notificationTime: LocalTime = LocalTime.of(9,0),
    val journeyReminderEnabled: Boolean = false,
    val liveLocationSharingEnabled: Boolean = false
)
