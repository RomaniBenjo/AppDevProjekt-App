package com.example.commingsoon.ui.screens.localopenguesser

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.io.File

/** The map-only part of Local OpenGuesser. Every map resource is stored on-device. */
@Composable
fun LocalOpenGuesserScreen(navController: NavHostController) {
    Box(modifier = Modifier.fillMaxSize()) {
        OfflineMap(modifier = Modifier.fillMaxSize())

        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            )
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text("Local OpenGuesser", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Offline map · no internet required",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(50)
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
            Text("Back to photo statistics", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun OfflineMap(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                Thread {
                    runCatching {
                        val archiveUrl = prepareLocalPmTiles(context)
                        context.assets.open(OFFLINE_STYLE_ASSET).bufferedReader().use { reader ->
                            reader.readText().replace(ARCHIVE_URL_PLACEHOLDER, archiveUrl)
                        }
                    }.onSuccess { styleJson ->
                        post {
                            map.setStyle(Style.Builder().fromJson(styleJson))
                            map.cameraPosition = CameraPosition.Builder()
                                .target(LatLng(47.5162, 14.5501))
                                .zoom(5.0)
                                .build()
                        }
                    }.onFailure { error ->
                        post { errorMessage = error.message ?: "Could not load the offline map" }
                    }
                }.start()
            }
        }
    }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    Box(modifier = modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        errorMessage?.let { message ->
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Text(message, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

private const val LOCAL_PMTILES_ASSET = "maps/world_z7.pmtiles"
private const val OFFLINE_STYLE_ASSET = "maps/offline_map_style.json"
private const val ARCHIVE_URL_PLACEHOLDER = "pmtiles://LOCAL_ARCHIVE"

private fun prepareLocalPmTiles(context: Context): String {
    val mapsDirectory = File(context.filesDir, "maps").apply { mkdirs() }
    val localFile = File(mapsDirectory, "world_z7.pmtiles")
    val assetLength = context.assets.openFd(LOCAL_PMTILES_ASSET).length

    if (!localFile.exists() || localFile.length() != assetLength) {
        val temporaryFile = File(mapsDirectory, "world_z7.pmtiles.tmp")
        context.assets.open(LOCAL_PMTILES_ASSET).use { input ->
            temporaryFile.outputStream().buffered().use { output -> input.copyTo(output) }
        }
        check(temporaryFile.renameTo(localFile)) { "Could not prepare the offline map archive" }
    }
    return "pmtiles://file://${localFile.absolutePath}"
}
