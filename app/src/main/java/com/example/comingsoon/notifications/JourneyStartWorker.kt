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
        val repository = AppPreferenceRepository(applicationContext)
        val settings = runBlocking { repository.settingsFlow.first() }
        val localizedContext = applicationContext.localized(settings.language)
        NotificationsHelper(localizedContext).showJourneyStarted(journeyName)

        return Result.success()
    }
}