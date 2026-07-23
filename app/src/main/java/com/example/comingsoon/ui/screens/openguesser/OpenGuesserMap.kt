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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.json.JSONObject
import org.json.JSONArray
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
    val isDarkMap = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accentColor = MaterialTheme.colorScheme.primary.toArgb()
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
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(20.0, 0.0))
                    .zoom(1.5)
                    .build()
            }
        }
    }

    LaunchedEffect(mapController, isDarkMap, accentColor) {
        val map = mapController ?: return@LaunchedEffect
        isStyleReady = false
        Thread {
            runCatching {
                val sourceUrl = currentArchiveUrl()
                context.assets.open(MAP_STYLE_ASSET).bufferedReader().use { reader ->
                    themedMapStyle(
                        reader.readText().replace(ARCHIVE_URL_PLACEHOLDER, sourceUrl),
                        isDarkMap,
                        accentColor
                    )
                }
            }.onSuccess { styleJson ->
                mapView.post {
                    map.setStyle(Style.Builder().fromJson(styleJson)) {
                        guessMarker = null
                        actualMarker = null
                        resultLine = null
                        playerMarkers = emptyList()
                        playerLines = emptyList()
                        isStyleReady = true
                    }
                }
            }.onFailure { mapView.post { errorMessage = mapLoadError } }
        }.start()
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
                        labeledMarkerBitmap(
                            result.label,
                            context.resources.displayMetrics.density,
                            accentColor
                        )
                    ))
            )
        }
        playerLines = if (actualLocation != null) {
            resultMarkers.map { result ->
                map.addPolyline(
                    PolylineOptions()
                        .add(result.location, actualLocation)
                        .color(accentColor)
                        .width(4f)
                )
            }
        } else emptyList()
        resultLine = if (selectedLocation != null && actualLocation != null) {
            map.addPolyline(
                PolylineOptions()
                    .add(selectedLocation, actualLocation)
                    .color(accentColor)
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

private fun labeledMarkerBitmap(label: String, density: Float, accentColor: Int): Bitmap {
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (Color.luminance(accentColor) > 0.5f) Color.BLACK else Color.WHITE
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
    val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
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

private fun themedMapStyle(styleJson: String, dark: Boolean, accent: Int): String {
    val style = JSONObject(styleJson)
    remapColors(style, dark)
    style.put("font-faces", JSONObject().put(MAP_FONT_NAME, MAP_FONT_URL))
    val layers = style.getJSONArray("layers")
    val accentHex = colorHex(accent)
    val waterHex = blendHex(if (dark) "#252a30" else "#d9e4eb", accentHex, 0.28f)
    for (index in 0 until layers.length()) {
        val layer = layers.getJSONObject(index)
        val id = layer.optString("id")
        val paint = layer.optJSONObject("paint") ?: continue
        if (id.startsWith("water")) {
            if (paint.has("fill-color")) paint.put("fill-color", waterHex)
            if (paint.has("line-color")) paint.put("line-color", waterHex)
        } else if (id.startsWith("boundaries")) {
            paint.put(
                "line-color",
                blendHex(if (dark) "#8a9198" else "#687078", accentHex, 0.45f)
            )
        }
    }
    layers.put(labelLayer(CITY_LABEL_LAYER, dark))
        .put(labelLayer(TOWN_LABEL_LAYER, dark))
    return style.toString()
}

private fun labelLayer(json: String, dark: Boolean): JSONObject = JSONObject(json).apply {
    getJSONObject("paint").apply {
        put("text-color", if (dark) "#f1f3f4" else "#202124")
        put("text-halo-color", if (dark) "#17191c" else "#fafafa")
    }
}

private fun remapColors(value: Any, dark: Boolean) {
    when (value) {
        is JSONObject -> value.keys().forEach { key ->
            val child = value.get(key)
            if (child is String && child.matches(Regex("#[0-9a-fA-F]{6}"))) {
                value.put(key, remappedGray(child, dark))
            } else {
                remapColors(child, dark)
            }
        }
        is JSONArray -> for (index in 0 until value.length()) {
            val child = value.get(index)
            if (child is String && child.matches(Regex("#[0-9a-fA-F]{6}"))) {
                value.put(index, remappedGray(child, dark))
            } else {
                remapColors(child, dark)
            }
        }
    }
}

private fun remappedGray(color: String, dark: Boolean): String {
    val sourceLevel = color.substring(1, 3).toInt(16) / 255f
    val level = if (dark) 18 + (sourceLevel * 62).toInt()
    else 246 - (sourceLevel * 70).toInt()
    return "#%02x%02x%02x".format(level, level, level)
}

private fun colorHex(color: Int) = "#%02x%02x%02x".format(
    Color.red(color), Color.green(color), Color.blue(color)
)

private fun blendHex(base: String, accent: String, amount: Float): String {
    fun channel(value: String, offset: Int) = value.substring(offset, offset + 2).toInt(16)
    fun mixed(offset: Int) =
        (channel(base, offset) * (1f - amount) + channel(accent, offset) * amount).toInt()
    return "#%02x%02x%02x".format(mixed(1), mixed(3), mixed(5))
}
