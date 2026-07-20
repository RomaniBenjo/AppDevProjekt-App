package com.example.commingsoon.friends

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.net.URI

object FriendQrPayload {
    private const val SCHEME = "comingsoon"
    private const val HOST = "friend"

    fun create(userId: Int): String {
        require(userId > 0)
        return "$SCHEME://$HOST/$userId"
    }

    fun parse(value: String?): Int? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            val uri = URI(value.trim())
            val segments = uri.path.orEmpty().split('/').filter(String::isNotBlank)
            if (
                uri.scheme != SCHEME ||
                uri.host != HOST ||
                uri.query != null ||
                uri.fragment != null ||
                segments.size != 1
            ) {
                null
            } else {
                segments.single().toIntOrNull()?.takeIf { it > 0 }
            }
        }.getOrNull()
    }
}

fun createFriendQrBitmap(userId: Int, sizePx: Int): Bitmap {
    val matrix = QRCodeWriter().encode(
        FriendQrPayload.create(userId),
        BarcodeFormat.QR_CODE,
        sizePx,
        sizePx,
        mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        )
    )
    val pixels = IntArray(sizePx * sizePx)
    for (y in 0 until sizePx) {
        for (x in 0 until sizePx) {
            pixels[y * sizePx + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
        }
    }
    return Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
    }
}
