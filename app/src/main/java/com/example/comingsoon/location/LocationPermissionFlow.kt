package com.example.comingsoon.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.comingsoon.R
import com.example.comingsoon.language.appString

fun hasForegroundLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}

fun hasBackgroundLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

/**
 * Android forbids requesting background location together with foreground location in a
 * single prompt, and many OEMs don't surface an in-app "Allow all the time" option at all —
 * this drives the two-step dialog -> request -> (settings fallback) flow needed for live
 * location sharing. Returns a function to call to kick off the flow; renders its own dialogs.
 */
@Composable
fun rememberLiveLocationPermissionFlow(onAllGranted: () -> Unit): () -> Unit {
    val context = LocalContext.current
    var showForegroundRationale by remember { mutableStateOf(false) }
    var showBackgroundRationale by remember { mutableStateOf(false) }
    var showSettingsPrompt by remember { mutableStateOf(false) }

    val backgroundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onAllGranted() else showSettingsPrompt = true
    }

    val foregroundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            if (hasBackgroundLocationPermission(context)) {
                onAllGranted()
            } else {
                showBackgroundRationale = true
            }
        }
    }

    if (showForegroundRationale) {
        AlertDialog(
            onDismissRequest = { showForegroundRationale = false },
            title = { Text(appString(R.string.location_permission_title)) },
            text = { Text(appString(R.string.location_permission_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showForegroundRationale = false
                    foregroundLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                }) { Text(appString(R.string.continue_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showForegroundRationale = false }) { Text(appString(R.string.cancel)) }
            }
        )
    }

    if (showBackgroundRationale) {
        AlertDialog(
            onDismissRequest = { showBackgroundRationale = false },
            title = { Text(appString(R.string.background_location_title)) },
            text = { Text(appString(R.string.background_location_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showBackgroundRationale = false
                    backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }) { Text(appString(R.string.continue_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showBackgroundRationale = false }) { Text(appString(R.string.cancel)) }
            }
        )
    }

    if (showSettingsPrompt) {
        AlertDialog(
            onDismissRequest = { showSettingsPrompt = false },
            title = { Text(appString(R.string.permission_required_title)) },
            text = { Text(appString(R.string.permission_settings_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showSettingsPrompt = false
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    )
                }) { Text(appString(R.string.open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsPrompt = false }) { Text(appString(R.string.cancel)) }
            }
        )
    }

    return {
        if (hasForegroundLocationPermission(context) && hasBackgroundLocationPermission(context)) {
            onAllGranted()
        } else if (hasForegroundLocationPermission(context)) {
            showBackgroundRationale = true
        } else {
            showForegroundRationale = true
        }
    }
}
