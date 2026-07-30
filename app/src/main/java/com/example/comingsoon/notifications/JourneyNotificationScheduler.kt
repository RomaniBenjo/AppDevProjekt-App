package com.example.comingsoon.notifications

import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime

object JourneyNotificationScheduler {
    fun scheduleJourneyReminder(
        context: android.content.Context,
        journeyId: Int,
        journeyName: String,
        reminderTime: LocalDateTime
    ) {
        val delay = Duration.between(LocalDateTime.now(),reminderTime)
        if (delay.isNegative || delay.isZero) {
            cancel(context, journeyId)
            return
        }

        val data = Data.Builder()
            .putString("journey_name", journeyName)
            .putInt("journey_id", journeyId)
            .build()

        val request = OneTimeWorkRequestBuilder<JourneyStartWorker>()
            .setInitialDelay(delay)
            .setInputData(data)
            .addTag(JOURNEY_WORK_TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                journeyReminderWorkName(journeyId),
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

    fun cancel(context: android.content.Context, journeyId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork(journeyReminderWorkName(journeyId))
    }

    fun cancelAll(context: android.content.Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(JOURNEY_WORK_TAG)
    }

    private const val JOURNEY_WORK_TAG = "journey_reminders"
}

internal fun journeyReminderWorkName(journeyId: Int): String = "journey_reminder_$journeyId"
