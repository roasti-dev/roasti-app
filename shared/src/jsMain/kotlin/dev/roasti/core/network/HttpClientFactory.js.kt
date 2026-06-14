package dev.roasti.core.network

import dev.roasti.core.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// jsMain
actual fun createHttpClient(
    accessTokenProvider: () -> String?,
    baseHost: String,
    useHttps: Boolean,
): HttpClient = HttpClient(Js) {
    expectSuccess = true
    println("baseHost: $baseHost, useHttps: $useHttps")
    defaultRequest {
        host = baseHost
        url { protocol = if (useHttps) URLProtocol.HTTPS else URLProtocol.HTTP }

        contentType(ContentType.Application.Json)

        if (!url.encodedPath.startsWith(ApiRoutes.AuthPathPrefix)) {
            accessTokenProvider()?.let { accessToken ->
                header(HttpHeaders.Authorization, NetworkHeaders.BearerPrefix + accessToken)
            }
        }
    }
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            },
        )
    }
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                console.log(message)
            }
        }
        level = LogLevel.ALL
    }
}