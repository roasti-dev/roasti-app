package dev.roasti.core.network

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
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
import dev.roasti.core.config.AppConfig

private const val KtorLogTag = "KtorHttp"


actual fun createHttpClient(
    accessTokenProvider: () -> String?,
): HttpClient = HttpClient(OkHttp) {
    expectSuccess = true

    defaultRequest {
        host = AppConfig.BASE_HOST
        url {
            protocol = URLProtocol.HTTPS
        }

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
                Log.d(KtorLogTag, message)
            }
        }
        level = LogLevel.ALL
    }
}
