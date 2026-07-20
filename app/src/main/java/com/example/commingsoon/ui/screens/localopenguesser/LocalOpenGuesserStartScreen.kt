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
import com.example.commingsoon.R
import com.example.commingsoon.language.appString
import com.example.commingsoon.navigation.NavScreens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LocalOpenGuesserStartScreen(navController: NavHostController) {
    val context = LocalContext.current
    var access by remember { mutableStateOf(readPhotoAccess(context)) }
    var scanRequest by remember { mutableIntStateOf(0) }
    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf(PhotoScanProgress(0, 0)) }
    var stats by remember { mutableStateOf<PhotoLibraryStats?>(null) }
    var scanFailed by remember { mutableStateOf(false) }
    var mapDownloaded by remember { mutableStateOf(isOfflineMapDownloaded(context)) }
    var mapDownloadRequest by remember { mutableIntStateOf(0) }
    var isDownloadingMap by remember { mutableStateOf(false) }
    var mapDownloadProgress by remember { mutableStateOf<OfflineMapDownloadProgress?>(null) }
    var mapDownloadFailed by remember { mutableStateOf(false) }

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
        scanFailed = false
        runCatching {
            scanPhotoLibrary(context) { progress -> scanProgress = progress }
        }
            .onSuccess { stats = it }
            .onFailure { scanFailed = true }
        isScanning = false
    }

    LaunchedEffect(mapDownloadRequest) {
        if (mapDownloadRequest == 0 || mapDownloaded) return@LaunchedEffect
        isDownloadingMap = true
        mapDownloadFailed = false
        mapDownloadProgress = OfflineMapDownloadProgress(0L, null)
        runCatching {
            downloadOfflineMap(context) { progress ->
                withContext(Dispatchers.Main.immediate) {
                    mapDownloadProgress = progress
                }
            }
        }
            .onSuccess { mapDownloaded = true }
            .onFailure { mapDownloadFailed = true }
        isDownloadingMap = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(appString(R.string.local_guesser), style = MaterialTheme.typography.headlineSmall)
        Text(
            appString(R.string.local_guesser_start_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PermissionCard(access = access)

        OfflineMapDownloadCard(
            isDownloaded = mapDownloaded,
            isDownloading = isDownloadingMap,
            progress = mapDownloadProgress,
            downloadFailed = mapDownloadFailed,
            onDownload = { mapDownloadRequest++ }
        )

        Button(
            onClick = { permissionLauncher.launch(requiredPhotoPermissions()) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (access.isReady) {
                    appString(R.string.local_guesser_review_photo_permissions)
                } else {
                    appString(R.string.local_guesser_allow_library_and_scan)
                }
            )
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
                        Text(appString(R.string.local_guesser_scanning_photos))
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
                            appString(
                                R.string.local_guesser_scan_image_progress,
                                scanProgress.processedImages,
                                scanProgress.totalImages
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            appString(
                                R.string.local_guesser_percentage,
                                (scanProgress.fraction * 100).toInt()
                            ),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    if (scanProgress.processedImages > 0) {
                        Text(
                            appString(
                                R.string.local_guesser_scan_index_progress,
                                scanProgress.reusedFromIndex,
                                scanProgress.scannedNow
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        if (scanFailed) {
            Text(
                appString(R.string.local_guesser_scan_failed),
                color = MaterialTheme.colorScheme.error
            )
        }

        stats?.let { result ->
            PhotoStats(result)
            OutlinedButton(
                onClick = { scanRequest++ },
                enabled = !isScanning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(appString(R.string.local_guesser_scan_again))
            }
        }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { navController.navigate(NavScreens.OpenGuesserLocalLobby.route) },
            enabled = stats != null && !isScanning && mapDownloaded && !isDownloadingMap,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(appString(R.string.local_guesser_connect_phone))
        }
        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(appString(R.string.back))
        }
    }
}

@Composable
private fun OfflineMapDownloadCard(
    isDownloaded: Boolean,
    isDownloading: Boolean,
    progress: OfflineMapDownloadProgress?,
    downloadFailed: Boolean,
    onDownload: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                appString(R.string.local_guesser_offline_world_map),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                if (isDownloaded) {
                    appString(R.string.local_guesser_offline_map_ready)
                } else {
                    appString(R.string.local_guesser_offline_map_description)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isDownloading) {
                val fraction = progress?.fraction
                if (fraction != null) {
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                val downloaded = progress?.downloadedBytes ?: 0L
                val total = progress?.totalBytes
                Text(
                    if (total != null) {
                        appString(
                            R.string.local_guesser_downloading_progress,
                            formatFileSize(downloaded),
                            formatFileSize(total)
                        )
                    } else {
                        appString(
                            R.string.local_guesser_downloading_amount,
                            formatFileSize(downloaded)
                        )
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (downloadFailed) {
                Text(
                    appString(R.string.local_guesser_map_download_failed),
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (!isDownloaded) {
                Button(
                    onClick = onDownload,
                    enabled = !isDownloading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (isDownloading) {
                            appString(R.string.local_guesser_downloading_map)
                        } else {
                            appString(R.string.local_guesser_download_offline_map)
                        }
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

@Composable
private fun PermissionCard(access: PhotoAccess) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                appString(R.string.local_guesser_photo_access),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                if (access.hasFullLibrary) {
                    appString(R.string.local_guesser_entire_library_granted)
                } else {
                    appString(R.string.local_guesser_entire_library_missing)
                }
            )
            Text(
                if (access.hasLocationMetadata) {
                    appString(R.string.local_guesser_gps_metadata_granted)
                } else {
                    appString(R.string.local_guesser_gps_metadata_missing)
                }
            )
            if (access.hasLimitedLibrary && !access.hasFullLibrary) {
                Text(
                    appString(R.string.local_guesser_limited_library_warning),
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
            Text(
                appString(R.string.local_guesser_library_statistics),
                style = MaterialTheme.typography.titleMedium
            )
            StatRow(appString(R.string.local_guesser_images_found), stats.totalImages)
            StatRow(appString(R.string.local_guesser_with_gps), stats.imagesWithLocation)
            StatRow(appString(R.string.local_guesser_without_gps), stats.imagesWithoutLocation)
            if (stats.unresolvedLocations > 0) {
                StatRow(
                    appString(R.string.local_guesser_location_outside_country_data),
                    stats.unresolvedLocations
                )
            }
            if (stats.unreadableImages > 0) {
                StatRow(appString(R.string.local_guesser_unreadable_images), stats.unreadableImages)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            StatRow(appString(R.string.local_guesser_reused_from_index), stats.reusedFromIndex)
            StatRow(appString(R.string.local_guesser_scanned_now), stats.scannedNow)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                appString(R.string.local_guesser_images_by_country),
                style = MaterialTheme.typography.titleSmall
            )
            if (stats.imagesByCountry.isEmpty()) {
                Text(appString(R.string.local_guesser_no_country_photos))
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
