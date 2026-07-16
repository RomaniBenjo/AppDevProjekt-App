package com.example.commingsoon.notifications

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class JourneyStartWorker (
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    override fun doWork(): Result {
        val journeyName = inputData.getString("journey_name") ?: return Result.failure()

        NotificationsHelper(applicationContext).showJourneyStarted(journeyName)

        return Result.success()
    }
}