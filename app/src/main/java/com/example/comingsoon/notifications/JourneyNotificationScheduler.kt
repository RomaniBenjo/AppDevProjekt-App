package com.example.comingsoon.notifications

import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime

object JourneyNotificationScheduler {
    private const val JOURNEY_WORK = "journey_reminder"

    fun scheduleJourneyReminder(
        context: android.content.Context,
        journeyName: String,
        reminderTime: LocalDateTime
    ) {
        val delay = Duration.between(LocalDateTime.now(),reminderTime)
        if (delay.isNegative) { return }

        val data = Data.Builder().putString("journey_name", journeyName).build()

        val request = OneTimeWorkRequestBuilder<JourneyStartWorker>().setInitialDelay(delay).setInputData(data).build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                JOURNEY_WORK,
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

    fun cancelAll(context: android.content.Context) {
        WorkManager.getInstance(context).cancelUniqueWork(JOURNEY_WORK)
    }
}