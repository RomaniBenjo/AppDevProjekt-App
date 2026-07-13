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
    val reusedFromIndex: Int,
    val scannedNow: Int,
    val imagesByCountry: Map<String, Int>
)

internal data class PhotoScanProgress(
    val processedImages: Int,
    val totalImages: Int,
    val reusedFromIndex: Int = 0,
    val scannedNow: Int = 0
) {
    val fraction: Float
        get() = if (totalImages == 0) 0f else processedImages.toFloat() / totalImages
}

internal suspend fun scanPhotoLibrary(
    context: Context,
    onProgress: (PhotoScanProgress) -> Unit = {}
): PhotoLibraryStats = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val countryResolver by lazy { OfflineCountryResolver.load(context) }
    val indexDatabase = PhotoIndexDatabase(context)
    val cachedPhotos = indexDatabase.readAll()
    val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATE_MODIFIED,
        MediaStore.Images.Media.SIZE
    )
    var total = 0
    var reusedFromIndex = 0
    var scannedNow = 0
    val activeMediaIds = mutableSetOf<Long>()
    val changedPhotos = mutableListOf<IndexedPhoto>()
    val currentPhotos = mutableListOf<IndexedPhoto>()

    resolver.query(collection, projection, null, null, null)?.use { cursor ->
        val expectedTotal = cursor.count
        val updateEvery = (expectedTotal / 100).coerceAtLeast(1)
        withContext(Dispatchers.Main.immediate) {
            onProgress(PhotoScanProgress(processedImages = 0, totalImages = expectedTotal))
        }
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
        while (cursor.moveToNext()) {
            coroutineContext.ensureActive()
            total++
            val mediaId = cursor.getLong(idColumn)
            val dateModified = cursor.getLong(modifiedColumn)
            val size = cursor.getLong(sizeColumn)
            activeMediaIds += mediaId

            val cached = cachedPhotos[mediaId]
            val indexedPhoto = if (cached?.matches(dateModified, size) == true) {
                reusedFromIndex++
                cached
            } else {
                scannedNow++
                readPhotoIndex(
                    mediaId = mediaId,
                    dateModified = dateModified,
                    size = size,
                    collection = collection,
                    context = context,
                    countryResolver = countryResolver
                ).also(changedPhotos::add)
            }
            currentPhotos += indexedPhoto

            if (total % updateEvery == 0 || total == expectedTotal) {
                withContext(Dispatchers.Main.immediate) {
                    onProgress(
                        PhotoScanProgress(
                            processedImages = total,
                            totalImages = expectedTotal,
                            reusedFromIndex = reusedFromIndex,
                            scannedNow = scannedNow
                        )
                    )
                }
            }
        }
    }
    indexDatabase.applyChanges(changedPhotos, activeMediaIds)

    val withLocation = currentPhotos.count { it.latitude != null && it.longitude != null }
    val unreadable = currentPhotos.count(IndexedPhoto::unreadable)
    val unresolved = currentPhotos.count {
        it.latitude != null && it.longitude != null && it.country == null
    }
    val countryCounts = currentPhotos.mapNotNull(IndexedPhoto::country)
        .groupingBy { it }
        .eachCount()

    PhotoLibraryStats(
        totalImages = total,
        imagesWithLocation = withLocation,
        imagesWithoutLocation = (total - withLocation - unreadable).coerceAtLeast(0),
        unresolvedLocations = unresolved,
        unreadableImages = unreadable,
        reusedFromIndex = reusedFromIndex,
        scannedNow = scannedNow,
        imagesByCountry = countryCounts.toList()
            .sortedByDescending { it.second }
            .toMap()
    )
}

private fun readPhotoIndex(
    mediaId: Long,
    dateModified: Long,
    size: Long,
    collection: android.net.Uri,
    context: Context,
    countryResolver: OfflineCountryResolver
): IndexedPhoto {
    val resolver = context.contentResolver
    val uri = ContentUris.withAppendedId(collection, mediaId)
    return try {
        val originalUri = MediaStore.setRequireOriginal(uri)
        resolver.openFileDescriptor(originalUri, "r")?.use { descriptor ->
            val exif = ExifInterface(descriptor.fileDescriptor)
            val coordinates = FloatArray(2)
            if (exif.getLatLong(coordinates)) {
                val latitude = coordinates[0].toDouble()
                val longitude = coordinates[1].toDouble()
                IndexedPhoto(
                    mediaId = mediaId,
                    dateModified = dateModified,
                    size = size,
                    latitude = latitude,
                    longitude = longitude,
                    country = countryResolver.countryAt(latitude, longitude),
                    unreadable = false
                )
            } else {
                IndexedPhoto(mediaId, dateModified, size, null, null, null, unreadable = false)
            }
        } ?: IndexedPhoto(mediaId, dateModified, size, null, null, null, unreadable = true)
    } catch (_: Exception) {
        IndexedPhoto(mediaId, dateModified, size, null, null, null, unreadable = true)
    }
}
