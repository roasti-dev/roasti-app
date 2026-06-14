package dev.roasti

import dev.roasti.core.network.createHttpClient
import dev.roasti.feature.auth.data.network.AuthApiClientImpl

@JsExport
fun createAuthApiClient(accessTokenProvider: () -> String?, baseHost: String, useHttps: Boolean): AuthApiClientJs =
    AuthApiClientJsImpl(AuthApiClientImpl(createHttpClient(accessTokenProvider, baseHost, useHttps)))