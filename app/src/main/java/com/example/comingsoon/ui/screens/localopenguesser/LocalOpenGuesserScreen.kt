package com.example.comingsoon.ui.screens.localopenguesser

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.comingsoon.R
import com.example.comingsoon.language.appString
import com.example.comingsoon.ui.screens.openguesser.OpenGuesserMap
import org.maplibre.android.geometry.LatLng

/** Offline-backed map used by an active Local OpenGuesser round. */
@Composable
internal fun OfflineGuessMap(
    selectedLocation: LatLng?,
    onLocationSelected: (LatLng) -> Unit,
    actualLocation: LatLng? = null,
    isGuessingEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val missingMapError = appString(R.string.local_guesser_download_map_before_playing)

    OpenGuesserMap(
        archiveUrl = {
            check(isOfflineMapDownloaded(context)) { missingMapError }
            "pmtiles://file://${offlineMapArchive(context).absolutePath}"
        },
        selectedLocation = selectedLocation,
        onLocationSelected = onLocationSelected,
        actualLocation = actualLocation,
        isGuessingEnabled = isGuessingEnabled,
        mapLoadError = appString(R.string.local_guesser_map_load_failed),
        guessMarkerTitle = appString(R.string.local_guesser_your_guess),
        actualMarkerTitle = appString(R.string.local_guesser_real_location),
        modifier = modifier
    )
}
