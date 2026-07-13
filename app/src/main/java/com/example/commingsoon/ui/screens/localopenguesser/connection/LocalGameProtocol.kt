package com.example.commingsoon.ui.screens.localopenguesser.connection

import org.json.JSONObject

internal object LocalGameProtocol {
    private const val VERSION = 1
    private const val TYPE_TEST_MESSAGE = "test_message"
    const val TYPE_GAME_SETTINGS = "game_settings"
    const val TYPE_PLAYER_READY = "player_ready"
    const val TYPE_ROUND_START = "round_start"
    const val TYPE_PHOTO_METADATA = "photo_metadata"
    const val TYPE_PHOTO_READY = "photo_ready"
    const val TYPE_ROUND_REVEAL = "round_reveal"
    const val TYPE_GAME_FINISHED = "game_finished"
    const val TYPE_GAME_ERROR = "game_error"

    fun encodeTestMessage(message: String): ByteArray = JSONObject()
        .put("version", VERSION)
        .put("type", TYPE_TEST_MESSAGE)
        .put("message", message)
        .toString()
        .toByteArray(Charsets.UTF_8)

    fun decodeTestMessage(bytes: ByteArray): String? = runCatching {
        val json = decode(bytes) ?: return@runCatching null
        if (json.optInt("version") != VERSION || json.optString("type") != TYPE_TEST_MESSAGE) {
            null
        } else {
            json.getString("message")
        }
    }.getOrNull()

    fun encode(type: String, values: Map<String, Any?> = emptyMap()): ByteArray {
        val json = JSONObject().put("version", VERSION).put("type", type)
        values.forEach { (key, value) -> json.put(key, value) }
        return json.toString().toByteArray(Charsets.UTF_8)
    }

    fun decode(bytes: ByteArray): JSONObject? = runCatching {
        JSONObject(bytes.toString(Charsets.UTF_8)).takeIf { it.optInt("version") == VERSION }
    }.getOrNull()
}
