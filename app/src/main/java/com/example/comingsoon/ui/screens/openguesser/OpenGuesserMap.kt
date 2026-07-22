package com.example.comingsoon.ui.screens.openguesser

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Polyline
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

internal data class OpenGuesserResultMarker(
    val location: LatLng,
    val label: String
)

/**
 * Shared OpenGuesser map renderer. The caller supplies either a local or remote PMTiles URL;
 * styling, labels, markers and lifecycle handling stay identical between game modes.
 */
@Composable
internal fun OpenGuesserMap(
    archiveUrl: () -> String,
    selectedLocation: LatLng?,
    onLocationSelected: (LatLng) -> Unit,
    mapLoadError: String,
    guessMarkerTitle: String,
    actualMarkerTitle: String,
    actualLocation: LatLng? = null,
    resultMarkers: List<OpenGuesserResultMarker> = emptyList(),
    isGuessingEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var mapController by remember { mutableStateOf<MapLibreMap?>(null) }
    var isStyleReady by remember { mutableStateOf(false) }
    var guessMarker by remember { mutableStateOf<Marker?>(null) }
    var actualMarker by remember { mutableStateOf<Marker?>(null) }
    var resultLine by remember { mutableStateOf<Polyline?>(null) }
    var playerMarkers by remember { mutableStateOf<List<Marker>>(emptyList()) }
    var playerLines by remember { mutableStateOf<List<Polyline>>(emptyList()) }
    val currentOnLocationSelected by rememberUpdatedState(onLocationSelected)
    val currentIsGuessingEnabled by rememberUpdatedState(isGuessingEnabled)
    val currentArchiveUrl by rememberUpdatedState(archiveUrl)
    val mapClickListener = remember {
        MapLibreMap.OnMapClickListener { location ->
            if (currentIsGuessingEnabled) currentOnLocationSelected(location)
            currentIsGuessingEnabled
        }
    }
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> view.parent?.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> view.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
            getMapAsync { map ->
                mapController = map
                map.addOnMapClickListener(mapClickListener)
                Thread {
                    runCatching {
                        val sourceUrl = currentArchiveUrl()
                        context.assets.open(MAP_STYLE_ASSET).bufferedReader().use { reader ->
                            addPlaceLabels(reader.readText().replace(ARCHIVE_URL_PLACEHOLDER, sourceUrl))
                        }
                    }.onSuccess { styleJson ->
                        post {
                            map.cameraPosition = CameraPosition.Builder()
                                .target(LatLng(20.0, 0.0))
                                .zoom(1.5)
                                .build()
                            map.setStyle(Style.Builder().fromJson(styleJson)) {
                                isStyleReady = true
                            }
                        }
                    }.onFailure {
                        post { errorMessage = mapLoadError }
                    }
                }.start()
            }
        }
    }

    LaunchedEffect(mapController, isStyleReady, selectedLocation, actualLocation, resultMarkers) {
        if (!isStyleReady) return@LaunchedEffect
        val map = mapController ?: return@LaunchedEffect
        guessMarker?.let(map::removeMarker)
        actualMarker?.let(map::removeMarker)
        resultLine?.let(map::removePolyline)
        playerMarkers.forEach(map::removeMarker)
        playerLines.forEach(map::removePolyline)
        guessMarker = selectedLocation?.let { location ->
            map.addMarker(MarkerOptions().position(location).title(guessMarkerTitle))
        }
        actualMarker = actualLocation?.let { location ->
            map.addMarker(MarkerOptions().position(location).title(actualMarkerTitle))
        }
        playerMarkers = resultMarkers.map { result ->
            map.addMarker(
                MarkerOptions()
                    .position(result.location)
                    .title(result.label)
                    .icon(IconFactory.getInstance(context).fromBitmap(
                        labeledMarkerBitmap(result.label, context.resources.displayMetrics.density)
                    ))
            )
        }
        playerLines = if (actualLocation != null) {
            resultMarkers.map { result ->
                map.addPolyline(
                    PolylineOptions()
                        .add(result.location, actualLocation)
                        .color(Color.rgb(255, 152, 0))
                        .width(4f)
                )
            }
        } else emptyList()
        resultLine = if (selectedLocation != null && actualLocation != null) {
            map.addPolyline(
                PolylineOptions()
                    .add(selectedLocation, actualLocation)
                    .color(android.graphics.Color.rgb(255, 152, 0))
                    .width(5f)
            )
        } else {
            null
        }
        if (resultMarkers.isNotEmpty() && actualLocation != null) {
            val bounds = LatLngBounds.Builder().include(actualLocation)
            resultMarkers.forEach { bounds.include(it.location) }
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 96))
        } else if (selectedLocation != null && actualLocation != null) {
            val bounds = LatLngBounds.Builder()
                .include(selectedLocation)
                .include(actualLocation)
                .build()
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 96))
        } else if (actualLocation != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(actualLocation, 7.0))
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
            mapController?.removeOnMapClickListener(mapClickListener)
            mapView.onDestroy()
        }
    }

    Box(modifier = modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        Card(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            )
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(onClick = { mapController?.animateCamera(CameraUpdateFactory.zoomIn()) }) {
                    Text("+")
                }
                Button(onClick = { mapController?.animateCamera(CameraUpdateFactory.zoomOut()) }) {
                    Text("−")
                }
            }
        }
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

private const val MAP_STYLE_ASSET = "maps/offline_map_style.json"
private const val ARCHIVE_URL_PLACEHOLDER = "pmtiles://LOCAL_ARCHIVE"
private const val MAP_FONT_NAME = "Roboto"
private const val MAP_FONT_URL = "file:///system/fonts/Roboto-Regular.ttf"

private fun labeledMarkerBitmap(label: String, density: Float): Bitmap {
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 14f * density
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val horizontalPadding = 10f * density
    val bubbleHeight = 30f * density
    val pointerHeight = 8f * density
    val width = (textPaint.measureText(label) + horizontalPadding * 2)
        .coerceAtLeast(54f * density).toInt()
    val bitmap = Bitmap.createBitmap(width, (bubbleHeight + pointerHeight).toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(40, 93, 165) }
    canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), bubbleHeight), 10f * density, 10f * density, background)
    val center = width / 2f
    canvas.drawPath(Path().apply {
        moveTo(center - 7f * density, bubbleHeight - 1f)
        lineTo(center, bubbleHeight + pointerHeight)
        lineTo(center + 7f * density, bubbleHeight - 1f)
        close()
    }, background)
    val baseline = bubbleHeight / 2f - (textPaint.ascent() + textPaint.descent()) / 2f
    canvas.drawText(label, (width - textPaint.measureText(label)) / 2f, baseline, textPaint)
    return bitmap
}

private val CITY_LABEL_LAYER = """
    {
      "id": "place_labels_city",
      "type": "symbol",
      "source": "protomaps",
      "source-layer": "places",
      "minzoom": 3,
      "filter": ["==", ["get", "kind_detail"], "city"],
      "layout": {
        "symbol-sort-key": ["get", "sort_rank"],
        "text-field": ["coalesce", ["get", "name:en"], ["get", "name"]],
        "text-font": ["Roboto"],
        "text-size": ["interpolate", ["linear"], ["zoom"], 3, 11, 8, 16],
        "text-padding": 4,
        "text-max-width": 8
      },
      "paint": {
        "text-color": "#e0e0e0",
        "text-halo-color": "#141414",
        "text-halo-width": 1.5
      }
    }
""".trimIndent()

private val TOWN_LABEL_LAYER = """
    {
      "id": "place_labels_town",
      "type": "symbol",
      "source": "protomaps",
      "source-layer": "places",
      "minzoom": 7,
      "filter": ["in", ["get", "kind_detail"], ["literal", ["town", "village"]]],
      "layout": {
        "symbol-sort-key": ["get", "sort_rank"],
        "text-field": ["coalesce", ["get", "name:en"], ["get", "name"]],
        "text-font": ["Roboto"],
        "text-size": ["interpolate", ["linear"], ["zoom"], 7, 11, 12, 14],
        "text-padding": 3,
        "text-max-width": 8
      },
      "paint": {
        "text-color": "#c7c7c7",
        "text-halo-color": "#141414",
        "text-halo-width": 1.25
      }
    }
""".trimIndent()

private fun addPlaceLabels(styleJson: String): String {
    val style = JSONObject(styleJson)
    style.put("font-faces", JSONObject().put(MAP_FONT_NAME, MAP_FONT_URL))
    style.getJSONArray("layers")
        .put(JSONObject(CITY_LABEL_LAYER))
        .put(JSONObject(TOWN_LABEL_LAYER))
    return style.toString()
}
