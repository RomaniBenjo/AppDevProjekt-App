package com.example.comingsoon.errors

import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Test

class AppApiExceptionTest {
    @Test
    fun `network cause is preserved through api exception`() {
        val exception = AppApiException(
            message = "request failed",
            cause = UnknownHostException("offline")
        )

        assertEquals(UserErrorKind.NO_NETWORK, exception.userErrorKind())
    }

    @Test
    fun `timeout cause receives a dedicated error kind`() {
        val exception = AppApiException(
            message = "request failed",
            cause = SocketTimeoutException("timeout")
        )

        assertEquals(UserErrorKind.TIMEOUT, exception.userErrorKind())
    }

    @Test
    fun `http status codes map to actionable error kinds`() {
        assertEquals(
            UserErrorKind.SESSION_EXPIRED,
            AppApiException("unauthorized", statusCode = 401).userErrorKind()
        )
        assertEquals(
            UserErrorKind.CONFLICT,
            AppApiException("conflict", statusCode = 409).userErrorKind()
        )
        assertEquals(
            UserErrorKind.SERVER_UNAVAILABLE,
            AppApiException("server", statusCode = 503).userErrorKind()
        )
    }
}
