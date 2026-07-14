package com.example.commingsoon.ui.screens.localopenguesser

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

internal const val OFFLINE_MAP_DOWNLOAD_URL = "https://benji.link/appDev/world_z7.pmtiles"

internal data class OfflineMapDownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long?
) {
    val fraction: Float?
        get() = totalBytes?.takeIf { it > 0L }
            ?.let { (downloadedBytes.toDouble() / it).coerceIn(0.0, 1.0).toFloat() }
}

internal fun offlineMapArchive(context: Context): File =
    File(File(context.filesDir, "maps"), "world_z7.pmtiles")

internal fun isOfflineMapDownloaded(context: Context): Boolean =
    isValidPmTilesArchive(offlineMapArchive(context))

internal suspend fun downloadOfflineMap(
    context: Context,
    onProgress: suspend (OfflineMapDownloadProgress) -> Unit
) = withContext(Dispatchers.IO) {
    val archive = offlineMapArchive(context)
    archive.parentFile?.mkdirs()
    val temporaryFile = File(archive.parentFile, "${archive.name}.download")
    temporaryFile.delete()

    val connection = (URL(OFFLINE_MAP_DOWNLOAD_URL).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 30_000
        instanceFollowRedirects = true
        requestMethod = "GET"
    }

    try {
        connection.connect()
        if (connection.responseCode !in 200..299) {
            throw IOException("Map download failed (HTTP ${connection.responseCode})")
        }

        val totalBytes = connection.contentLengthLong.takeIf { it > 0L }
        var downloadedBytes = 0L
        var lastReportedPercent = -1
        onProgress(OfflineMapDownloadProgress(0L, totalBytes))

        connection.inputStream.buffered().use { input ->
            temporaryFile.outputStream().buffered().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 8)
                while (true) {
                    coroutineContext.ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    downloadedBytes += count

                    val percent = totalBytes?.let { (downloadedBytes * 100L / it).toInt() }
                    if (percent == null || percent != lastReportedPercent) {
                        lastReportedPercent = percent ?: lastReportedPercent
                        onProgress(OfflineMapDownloadProgress(downloadedBytes, totalBytes))
                    }
                }
            }
        }

        if (totalBytes != null && temporaryFile.length() != totalBytes) {
            throw IOException("The map download was incomplete")
        }
        if (!isValidPmTilesArchive(temporaryFile)) {
            throw IOException("The downloaded file is not a valid PMTiles map")
        }
        if (archive.exists() && !archive.delete()) {
            throw IOException("Could not replace the existing offline map")
        }
        if (!temporaryFile.renameTo(archive)) {
            throw IOException("Could not save the offline map")
        }
        onProgress(OfflineMapDownloadProgress(archive.length(), archive.length()))
    } finally {
        connection.disconnect()
        if (temporaryFile.exists()) temporaryFile.delete()
    }
}

private fun isValidPmTilesArchive(file: File): Boolean {
    if (!file.isFile || file.length() < PMTILES_HEADER.size) return false
    return runCatching {
        file.inputStream().use { input ->
            val header = ByteArray(PMTILES_HEADER.size)
            input.read(header) == header.size && header.contentEquals(PMTILES_HEADER)
        }
    }.getOrDefault(false)
}

private val PMTILES_HEADER = byteArrayOf(
    'P'.code.toByte(),
    'M'.code.toByte(),
    'T'.code.toByte(),
    'i'.code.toByte(),
    'l'.code.toByte(),
    'e'.code.toByte(),
    's'.code.toByte(),
    3
)
