package com.example.comingsoon.sync

import java.net.URI

object JourneyShareLink {
    fun parse(value: String?): String? {
        val uri = runCatching { value?.let(::URI) }.getOrNull() ?: return null
        if (uri.scheme != "comingsoon" || uri.host != "journey-share") return null
        return uri.path?.removePrefix("/")?.takeIf { "/" !in it }?.takeIf {
            it.length in 32..128 && it.all { character ->
                character.isLetterOrDigit() || character == '-' || character == '_'
            }
        }
    }
}
