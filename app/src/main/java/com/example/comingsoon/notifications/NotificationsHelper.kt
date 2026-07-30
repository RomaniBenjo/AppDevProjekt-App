package com.example.comingsoon.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.comingsoon.R
import com.example.comingsoon.MainActivity
import com.example.comingsoon.language.localized
import com.example.comingsoon.language.persistedAppLanguage


class NotificationsHelper(
    private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "travel_notifications"
        const val LIVE_LOCATION_CHANNEL_ID = "live_location_sharing"
        const val LIVE_LOCATION_NOTIFICATION_ID = 3
        private const val JOURNEY_NOTIFICATION_BASE_ID = 10_000
        private const val COUNTRY_NOTIFICATION_ID = 2
        private const val TEST_NOTIFICATION_ID = 999
    }

    fun createNotificationChannel() {
        val localizedContext = context.localized(context.persistedAppLanguage())
        val channel = NotificationChannel(
            CHANNEL_ID,
            localizedContext.getString(R.string.notification_travel_channel),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = localizedContext.getString(R.string.notification_travel_channel_description)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    fun createLiveLocationNotificationChannel() {
        val localizedContext = context.localized(context.persistedAppLanguage())
        val channel = NotificationChannel(
            LIVE_LOCATION_CHANNEL_ID,
            localizedContext.getString(R.string.notification_live_location_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = localizedContext.getString(R.string.notification_live_location_channel_description)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    fun buildLiveLocationNotification(contentIntent: PendingIntent?, stopIntent: PendingIntent): Notification {
        val localizedContext = context.localized(context.persistedAppLanguage())
        return NotificationCompat.Builder(localizedContext, LIVE_LOCATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(localizedContext.getString(R.string.notification_live_location_title))
            .setContentText(localizedContext.getString(R.string.notification_live_location_text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(0, localizedContext.getString(R.string.notification_stop_sharing), stopIntent)
            .build()
    }

    fun showJourneyStarted(journeyId: Int, journeyName: String) {
        val localizedContext = context.localized(context.persistedAppLanguage())
        val notificationId = JOURNEY_NOTIFICATION_BASE_ID + (journeyId and 0x0FFF)
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        showNotification(
            id = notificationId,
            title = localizedContext.getString(R.string.notification_journey_title),
            text = localizedContext.getString(R.string.notification_journey_text, journeyName),
            contentIntent = contentIntent
        )
    }

    fun showCountryChanged(
        oldCountry: String,
        newCountry: String
    ) {
        val localizedContext = context.localized(context.persistedAppLanguage())
        showNotification(
            id = COUNTRY_NOTIFICATION_ID,
            title = localizedContext.getString(R.string.notification_country_title),
            text = localizedContext.getString(
                R.string.notification_country_text,
                oldCountry,
                newCountry
            )
        )
    }

    fun showTestNotification() {
        val localizedContext = context.localized(context.persistedAppLanguage())
        showNotification(
            id = TEST_NOTIFICATION_ID,
            title = localizedContext.getString(R.string.notification_test_title),
            text = localizedContext.getString(R.string.notification_test_text)
        )
    }

    private fun showNotification(
        id: Int,
        title: String,
        text: String,
        contentIntent: PendingIntent? = null
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
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
