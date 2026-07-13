package com.example.commingsoon.ui.screens.localopenguesser.connection

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

internal fun requiredNearbyPermissions(): Array<String> = buildList {
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.S) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        else -> add(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}.toTypedArray()

internal fun hasNearbyPermissions(context: Context): Boolean = requiredNearbyPermissions().all {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}
