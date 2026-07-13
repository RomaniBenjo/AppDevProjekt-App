package com.example.commingsoon.ui.screens.localopenguesser

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.commingsoon.navigation.NavScreens

@Composable
fun LocalOpenGuesserStartScreen(navController: NavHostController) {
    val context = LocalContext.current
    var access by remember { mutableStateOf(readPhotoAccess(context)) }
    var scanRequest by remember { mutableIntStateOf(0) }
    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf(PhotoScanProgress(0, 0)) }
    var stats by remember { mutableStateOf<PhotoLibraryStats?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        access = readPhotoAccess(context)
        scanRequest++
    }

    LaunchedEffect(access, scanRequest) {
        if (!access.hasFullLibrary || !access.hasLocationMetadata) return@LaunchedEffect
        isScanning = true
        scanProgress = PhotoScanProgress(0, 0)
        scanError = null
        runCatching {
            scanPhotoLibrary(context) { progress -> scanProgress = progress }
        }
            .onSuccess { stats = it }
            .onFailure { scanError = it.message ?: "The photo library could not be scanned." }
        isScanning = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Local OpenGuesser", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Allow access to your full photo library to find photos with GPS metadata. " +
                "The scan and country lookup happen only on this phone.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PermissionCard(access = access)

        Button(
            onClick = { permissionLauncher.launch(requiredPhotoPermissions()) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (access.isReady) "Review photo permissions" else "Allow full library and scan")
        }

        if (isScanning) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (scanProgress.totalImages == 0) {
                            CircularProgressIndicator()
                        }
                        Text("Scanning photos and reading GPS metadata…")
                    }
                    LinearProgressIndicator(
                        progress = { scanProgress.fraction },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${scanProgress.processedImages} / ${scanProgress.totalImages} images",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "${(scanProgress.fraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        scanError?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        stats?.let { result ->
            PhotoStats(result)
            OutlinedButton(
                onClick = { scanRequest++ },
                enabled = !isScanning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scan again")
            }
        }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { navController.navigate(NavScreens.OpenGuesserLocalMap.route) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open offline map")
        }
        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun PermissionCard(access: PhotoAccess) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Photo access", style = MaterialTheme.typography.titleMedium)
            Text(if (access.hasFullLibrary) "✓ Entire photo library" else "✗ Entire photo library")
            Text(if (access.hasLocationMetadata) "✓ Original GPS metadata" else "✗ Original GPS metadata")
            if (access.hasLimitedLibrary && !access.hasFullLibrary) {
                Text(
                    "Only selected photos are allowed. Choose “Allow all” in the Android dialog " +
                        "so the statistics represent the complete library.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun PhotoStats(stats: PhotoLibraryStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Library statistics", style = MaterialTheme.typography.titleMedium)
            StatRow("Images found", stats.totalImages)
            StatRow("With GPS location", stats.imagesWithLocation)
            StatRow("Without GPS location", stats.imagesWithoutLocation)
            if (stats.unresolvedLocations > 0) StatRow("GPS location outside country data", stats.unresolvedLocations)
            if (stats.unreadableImages > 0) StatRow("Could not be read", stats.unreadableImages)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Images by country", style = MaterialTheme.typography.titleSmall)
            if (stats.imagesByCountry.isEmpty()) {
                Text("No photos with a country location were found.")
            } else {
                stats.imagesByCountry.forEach { (country, count) -> StatRow(country, count) }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value.toString(), style = MaterialTheme.typography.labelLarge)
    }
}

private data class PhotoAccess(
    val hasFullLibrary: Boolean,
    val hasLimitedLibrary: Boolean,
    val hasLocationMetadata: Boolean
) {
    val isReady: Boolean get() = hasFullLibrary && hasLocationMetadata
}

private fun readPhotoAccess(context: Context): PhotoAccess {
    fun granted(permission: String) = ContextCompat.checkSelfPermission(
        context,
        permission
    ) == PackageManager.PERMISSION_GRANTED

    val fullAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        granted(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        granted(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    val limitedAccess = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)

    return PhotoAccess(
        hasFullLibrary = fullAccess,
        hasLimitedLibrary = limitedAccess,
        hasLocationMetadata = granted(Manifest.permission.ACCESS_MEDIA_LOCATION)
    )
}

private fun requiredPhotoPermissions(): Array<String> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        add(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    add(Manifest.permission.ACCESS_MEDIA_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    }
}.toTypedArray()
