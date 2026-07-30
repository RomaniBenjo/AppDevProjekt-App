package com.example.comingsoon.notifications

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.comingsoon.language.localized
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.example.comingsoon.data.AppPreferenceRepository

class JourneyStartWorker (
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    override fun doWork(): Result {
        val journeyName = inputData.getString("journey_name") ?: return Result.failure()
        val journeyId = inputData.getInt("journey_id", Int.MIN_VALUE)
        if (journeyId == Int.MIN_VALUE) return Result.failure()
        val repository = AppPreferenceRepository(applicationContext)
        val settings = runBlocking { repository.settingsFlow.first() }
        if (!settings.journeyReminderEnabled) return Result.success()
        val localizedContext = applicationContext.localized(settings.language)
        NotificationsHelper(localizedContext).showJourneyStarted(journeyId, journeyName)

        return Result.success()
    }
}
