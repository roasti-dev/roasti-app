package dev.roasti.core.network

import dev.roasti.core.config.AppConfig
import io.ktor.client.HttpClient

//  TODO: use config object
data class HttpClientConfig(
    val baseHost: String = AppConfig.BASE_HOST,
    val useHttps: Boolean = true,
)

expect fun createHttpClient(
    accessTokenProvider: () -> String?,
    baseHost: String = AppConfig.BASE_HOST,
    useHttps: Boolean = true,
): HttpClient
