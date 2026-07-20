package com.example.comingsoon.notifications

import android.Manifest
import android.os.Build

object NotificationPermission {
    val permission: String
        get() =
            if (Build.VERSION.SDK_INT >= 33) {
                Manifest.permission.POST_NOTIFICATIONS
            } else {
                ""
            }
}