package com.example.comingsoon.ui.screens.onlineopenguesser

import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.StreetViewPanorama
import com.google.android.gms.maps.StreetViewPanoramaView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.StreetViewSource

@Composable
fun StreetViewPanorama(
    latitude: Double,
    longitude: Double,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier,
    onPanoramaLoaded: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val panoramaView = remember { StreetViewPanoramaView(context) }
    var panorama by remember { mutableStateOf<StreetViewPanorama?>(null) }

    DisposableEffect(panoramaView, lifecycle) {
        panoramaView.onCreate(null)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> panoramaView.onStart()
                Lifecycle.Event.ON_RESUME -> panoramaView.onResume()
                Lifecycle.Event.ON_PAUSE -> panoramaView.onPause()
                Lifecycle.Event.ON_STOP -> panoramaView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        panoramaView.getStreetViewPanoramaAsync { panorama = it }

        onDispose {
            lifecycle.removeObserver(observer)
            panoramaView.onPause()
            panoramaView.onStop()
            panoramaView.onDestroy()
        }
    }

    LaunchedEffect(panorama, latitude, longitude) {
        panorama?.apply {
            isStreetNamesEnabled = false
            isPanningGesturesEnabled = true
            isZoomGesturesEnabled = true
            isUserNavigationEnabled = true
            setOnStreetViewPanoramaChangeListener { onPanoramaLoaded() }
            setPosition(LatLng(latitude, longitude), 1_000, StreetViewSource.OUTDOOR)
        }
    }

    AndroidView(
        factory = {
            panoramaView.apply {
                visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
            }
        },
        update = { view ->
            // Street View renders through a native surface, which is not affected by Compose
            // alpha or zIndex. Hide the Android view itself while retaining its loaded state.
            view.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
        },
        modifier = modifier.fillMaxSize()
    )
}
