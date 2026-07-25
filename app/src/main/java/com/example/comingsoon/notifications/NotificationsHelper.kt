package com.example.comingsoon.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
        const val LIVE_LOCATION_CHANNEL_ID = "live_location_sharing"
        const val LIVE_LOCATION_CHANNEL_NAME = "Live Location Sharing"
        const val LIVE_LOCATION_NOTIFICATION_ID = 3
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

    fun createLiveLocationNotificationChannel() {
        val channel = NotificationChannel(
            LIVE_LOCATION_CHANNEL_ID,
            LIVE_LOCATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shown while your live location is being shared with friends"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    fun buildLiveLocationNotification(contentIntent: PendingIntent?, stopIntent: PendingIntent): Notification {
        return NotificationCompat.Builder(context, LIVE_LOCATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Standort wird geteilt")
            .setContentText("Deine Freunde können deinen Live-Standort sehen.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(0, "Teilen stoppen", stopIntent)
            .build()
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