package com.example.comingsoon.ui.screens.livelocation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.comingsoon.BuildConfig
import com.example.comingsoon.location.circlePolygonPoints
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.Polygon
import org.maplibre.android.annotations.PolygonOptions
import org.maplibre.android.annotations.Polyline
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

data class LiveMapEntry(
    val label: String,
    val position: LatLng,
    val accuracyMeters: Float,
    val trail: List<LatLng>,
    val isSelf: Boolean = false
)

private const val MAP_STYLE_ASSET = "maps/offline_map_style.json"
private const val ARCHIVE_URL_PLACEHOLDER = "pmtiles://LOCAL_ARCHIVE"

@Composable
fun LiveLocationMap(
    entries: List<LiveMapEntry>,
    mapLoadError: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val isDarkMap = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val selfColor = MaterialTheme.colorScheme.primary.toArgb()
    val friendColor = MaterialTheme.colorScheme.secondary.toArgb()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var mapController by remember { mutableStateOf<MapLibreMap?>(null) }
    var isStyleReady by remember { mutableStateOf(false) }
    var markers by remember { mutableStateOf<List<Marker>>(emptyList()) }
    var circles by remember { mutableStateOf<List<Polygon>>(emptyList()) }
    var trailLines by remember { mutableStateOf<List<Polyline>>(emptyList()) }
    var hasFramedCamera by remember { mutableStateOf(false) }
    val currentEntries by rememberUpdatedState(entries)

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                mapController = map
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(20.0, 0.0))
                    .zoom(1.5)
                    .build()
            }
        }
    }

    LaunchedEffect(mapController, isDarkMap) {
        val map = mapController ?: return@LaunchedEffect
        isStyleReady = false
        Thread {
            runCatching {
                context.assets.open(MAP_STYLE_ASSET).bufferedReader().use { reader ->
                    reader.readText().replace(
                        ARCHIVE_URL_PLACEHOLDER,
                        "pmtiles://${BuildConfig.ONLINE_MAP_URL}"
                    )
                }
            }.onSuccess { styleJson ->
                mapView.post {
                    map.setStyle(Style.Builder().fromJson(styleJson)) {
                        markers = emptyList()
                        circles = emptyList()
                        trailLines = emptyList()
                        isStyleReady = true
                    }
                }
            }.onFailure { mapView.post { errorMessage = mapLoadError } }
        }.start()
    }

    LaunchedEffect(mapController, isStyleReady, entries) {
        if (!isStyleReady) return@LaunchedEffect
        val map = mapController ?: return@LaunchedEffect
        markers.forEach(map::removeMarker)
        circles.forEach(map::removePolygon)
        trailLines.forEach(map::removePolyline)

        val liveEntries = currentEntries
        markers = liveEntries.map { entry ->
            map.addMarker(MarkerOptions().position(entry.position).title(entry.label))
        }
        circles = liveEntries.map { entry ->
            val color = if (entry.isSelf) selfColor else friendColor
            map.addPolygon(
                PolygonOptions()
                    .addAll(circlePolygonPoints(entry.position, entry.accuracyMeters.toDouble()))
                    .fillColor(color)
                    .alpha(0.18f)
            )
        }
        trailLines = liveEntries.filter { it.trail.size > 1 }.map { entry ->
            map.addPolyline(
                PolylineOptions()
                    .addAll(entry.trail)
                    .color(if (entry.isSelf) selfColor else friendColor)
                    .width(4f)
            )
        }

        if (!hasFramedCamera && liveEntries.isNotEmpty()) {
            hasFramedCamera = true
            if (liveEntries.size == 1) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(liveEntries.first().position, 14.0))
            } else {
                val bounds = LatLngBounds.Builder()
                liveEntries.forEach { bounds.include(it.position) }
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 96))
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
