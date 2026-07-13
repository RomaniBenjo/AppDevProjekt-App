package com.example.commingsoon.ui.screens.localopenguesser

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

internal data class PhotoLibraryStats(
    val totalImages: Int,
    val imagesWithLocation: Int,
    val imagesWithoutLocation: Int,
    val unresolvedLocations: Int,
    val unreadableImages: Int,
    val imagesByCountry: Map<String, Int>
)

internal data class PhotoScanProgress(
    val processedImages: Int,
    val totalImages: Int
) {
    val fraction: Float
        get() = if (totalImages == 0) 0f else processedImages.toFloat() / totalImages
}

internal suspend fun scanPhotoLibrary(
    context: Context,
    onProgress: (PhotoScanProgress) -> Unit = {}
): PhotoLibraryStats = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val countryResolver = OfflineCountryResolver.load(context)
    val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    val projection = arrayOf(MediaStore.Images.Media._ID)
    var total = 0
    var withLocation = 0
    var unreadable = 0
    var unresolved = 0
    val countryCounts = mutableMapOf<String, Int>()

    resolver.query(collection, projection, null, null, null)?.use { cursor ->
        val expectedTotal = cursor.count
        val updateEvery = (expectedTotal / 100).coerceAtLeast(1)
        withContext(Dispatchers.Main.immediate) {
            onProgress(PhotoScanProgress(processedImages = 0, totalImages = expectedTotal))
        }
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while (cursor.moveToNext()) {
            coroutineContext.ensureActive()
            total++
            val uri = ContentUris.withAppendedId(collection, cursor.getLong(idColumn))
            try {
                val originalUri = MediaStore.setRequireOriginal(uri)
                resolver.openFileDescriptor(originalUri, "r")?.use { descriptor ->
                    val exif = ExifInterface(descriptor.fileDescriptor)
                    val coordinates = FloatArray(2)
                    if (exif.getLatLong(coordinates)) {
                        withLocation++
                        val country = countryResolver.countryAt(
                            latitude = coordinates[0].toDouble(),
                            longitude = coordinates[1].toDouble()
                        )
                        if (country == null) {
                            unresolved++
                        } else {
                            countryCounts[country] = countryCounts.getOrDefault(country, 0) + 1
                        }
                    }
                } ?: run { unreadable++ }
            } catch (_: Exception) {
                unreadable++
            }
            if (total % updateEvery == 0 || total == expectedTotal) {
                withContext(Dispatchers.Main.immediate) {
                    onProgress(PhotoScanProgress(processedImages = total, totalImages = expectedTotal))
                }
            }
        }
    }

    PhotoLibraryStats(
        totalImages = total,
        imagesWithLocation = withLocation,
        imagesWithoutLocation = (total - withLocation - unreadable).coerceAtLeast(0),
        unresolvedLocations = unresolved,
        unreadableImages = unreadable,
        imagesByCountry = countryCounts.toList()
            .sortedByDescending { it.second }
            .toMap()
    )
}
