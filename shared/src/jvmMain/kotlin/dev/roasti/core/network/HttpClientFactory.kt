package dev.roasti.core.network

import io.ktor.client.HttpClient

actual fun createHttpClient(accessTokenProvider: () -> String?): HttpClient =
    throw UnsupportedOperationException("createHttpClient not supported on JVM server")
