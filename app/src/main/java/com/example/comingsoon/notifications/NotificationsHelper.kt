package com.example.comingsoon.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.comingsoon.R


class NotificationsHelper(
    private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "travel_notifications"
        const val CHANNEL_NAME = "Travel Notifications"
        private const val JOURNEY_NOTIFICATION_ID = 1
        private const val COUNTRY_NOTIFICATION_ID = 2
        private const val TEST_NOTIFICATION_ID = 999
    }

    fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for journeys and travel updates"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    fun showJourneyStarted(journeyName: String) {
        showNotification(
            id = JOURNEY_NOTIFICATION_ID,
            title = context.getString(R.string.notification_journey_title),
            text = context.getString(R.string.notification_journey_text,journeyName)
        )
    }

    fun showCountryChanged(
        oldCountry: String,
        newCountry: String
    ) {
        showNotification(
            id = COUNTRY_NOTIFICATION_ID,
            title = context.getString(R.string.notification_country_title),
            text = context.getString(R.string.notification_country_text,oldCountry, newCountry)
        )
    }

    fun showTestNotification() {
        showNotification(
            id = TEST_NOTIFICATION_ID,
            title = context.getString(R.string.notification_test_title),
            text = context.getString(R.string.notification_test_text)
        )
    }

    private fun showNotification(
        id: Int,
        title: String,
        text: String
    ) {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }
}