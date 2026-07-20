package com.example.comingsoon.ui.screens.localopenguesser.connection

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.provider.MediaStore
import com.example.comingsoon.ui.screens.localopenguesser.IndexedPhoto
import java.io.File
import kotlin.math.roundToInt

/** Creates a resized JPEG without EXIF, so the answer is never transferred with the photo. */
internal fun preparePhotoForTransfer(
    context: Context,
    photo: IndexedPhoto,
    round: Int
): File {
    val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    val uri = ContentUris.withAppendedId(collection, photo.mediaId)
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        val width = info.size.width
        val height = info.size.height
        val scale = minOf(1f, MAX_IMAGE_EDGE.toFloat() / maxOf(width, height))
        decoder.setTargetSize(
            (width * scale).roundToInt().coerceAtLeast(1),
            (height * scale).roundToInt().coerceAtLeast(1)
        )
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
    val directory = File(context.cacheDir, "local_openguesser/outgoing").apply { mkdirs() }
    val output = File(directory, "round_${round}_${System.nanoTime()}.jpg")
    output.outputStream().buffered().use { stream ->
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)) {
            "Could not encode the selected photo"
        }
    }
    bitmap.recycle()
    return output
}

private const val MAX_IMAGE_EDGE = 1920
private const val JPEG_QUALITY = 88
