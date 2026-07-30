package com.example.comingsoon.errors

import android.content.Context
import androidx.annotation.StringRes
import com.example.comingsoon.R
import com.example.comingsoon.language.localizedString
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

open class AppApiException(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null
) : Exception(message, cause)

internal enum class UserErrorKind {
    NO_NETWORK,
    TIMEOUT,
    SESSION_EXPIRED,
    NOT_ALLOWED,
    NOT_FOUND,
    CONFLICT,
    EXPIRED,
    INVALID_DATA,
    SERVER_UNAVAILABLE,
    FALLBACK
}

internal fun Throwable.userErrorKind(): UserErrorKind {
    val causes = generateSequence(this as Throwable?) { it.cause }.toList()
    val apiError = causes.filterIsInstance<AppApiException>().firstOrNull()
    return when {
        causes.any {
            it is UnknownHostException ||
                it is ConnectException ||
                it is NoRouteToHostException
        } -> UserErrorKind.NO_NETWORK
        causes.any { it is SocketTimeoutException } -> UserErrorKind.TIMEOUT
        apiError?.statusCode == 401 -> UserErrorKind.SESSION_EXPIRED
        apiError?.statusCode == 403 -> UserErrorKind.NOT_ALLOWED
        apiError?.statusCode == 404 -> UserErrorKind.NOT_FOUND
        apiError?.statusCode == 409 -> UserErrorKind.CONFLICT
        apiError?.statusCode == 410 -> UserErrorKind.EXPIRED
        apiError?.statusCode == 422 -> UserErrorKind.INVALID_DATA
        apiError?.statusCode != null && apiError.statusCode >= 500 ->
            UserErrorKind.SERVER_UNAVAILABLE
        else -> UserErrorKind.FALLBACK
    }
}

fun Throwable.localizedUserMessage(
    context: Context,
    @StringRes fallback: Int,
    @StringRes conflict: Int = R.string.error_conflict
): String {
    val messageResource = when (userErrorKind()) {
        UserErrorKind.NO_NETWORK -> R.string.error_no_network
        UserErrorKind.TIMEOUT -> R.string.error_timeout
        UserErrorKind.SESSION_EXPIRED -> R.string.error_session_expired
        UserErrorKind.NOT_ALLOWED -> R.string.error_not_allowed
        UserErrorKind.NOT_FOUND -> R.string.error_not_found
        UserErrorKind.CONFLICT -> conflict
        UserErrorKind.EXPIRED -> R.string.error_expired
        UserErrorKind.INVALID_DATA -> R.string.error_invalid_data
        UserErrorKind.SERVER_UNAVAILABLE -> R.string.error_server_unavailable
        UserErrorKind.FALLBACK -> fallback
    }
    return context.localizedString(messageResource)
}
