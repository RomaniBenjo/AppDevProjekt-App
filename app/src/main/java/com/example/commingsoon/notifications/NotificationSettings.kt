package com.example.commingsoon.notifications

import java.time.LocalTime

data class NotificationSettings(
    val journeyReminderEnabled: Boolean = true,
    val reminderTime: LocalTime = LocalTime.of(9, 0)
)
