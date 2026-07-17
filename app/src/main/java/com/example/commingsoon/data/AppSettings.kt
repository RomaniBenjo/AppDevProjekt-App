package com.example.commingsoon.data

import com.example.commingsoon.language.AppLanguage
import com.example.commingsoon.ui.theme.AppThemeType
import java.time.LocalTime

data class AppSettings(
    val darkMode: Boolean = false,
    val theme: AppThemeType = AppThemeType.PINK,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val notificationTime: LocalTime = LocalTime.of(9,0),
    val journeyReminderEnabled: Boolean = false
)
