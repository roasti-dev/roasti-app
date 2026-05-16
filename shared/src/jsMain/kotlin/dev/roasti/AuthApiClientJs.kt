package dev.roasti

import dev.roasti.feature.auth.data.network.AuthApiClient
import dev.roasti.feature.auth.data.network.model.request.LoginRequestDto
import dev.roasti.feature.auth.data.network.model.request.RegisterRequestDto
import dev.roasti.feature.auth.data.network.model.response.AuthResponseDto
import dev.roasti.feature.auth.data.network.model.response.RefreshResponseDto

@JsExport
interface AuthApiClientJs {
    suspend fun login(request: LoginRequestDto): AuthResponseDto
    suspend fun register(request: RegisterRequestDto): AuthResponseDto
    suspend fun logout(accessToken: String)
    suspend fun refresh(refreshToken: String): RefreshResponseDto
}


class AuthApiClientJsImpl(private val client: AuthApiClient): AuthApiClientJs {


    override suspend fun login(request: LoginRequestDto): AuthResponseDto =
        client.login(request).getOrThrow()

    override suspend fun register(request: RegisterRequestDto): AuthResponseDto =
        client.register(request).getOrThrow()

    override suspend fun logout(accessToken: String): Unit =
        client.logout(accessToken).getOrThrow()

    override suspend fun refresh(refreshToken: String): RefreshResponseDto =
        client.refresh(refreshToken).getOrThrow()
}