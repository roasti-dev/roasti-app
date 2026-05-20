package dev.roasti.feature.auth.data.network

import dev.roasti.core.network.ApiRoutes
import dev.roasti.core.network.AuthorizedRequestExecutor
import dev.roasti.feature.auth.data.network.model.request.UpdateProfileRequest
import dev.roasti.feature.auth.data.network.model.response.PublicUserDto
import dev.roasti.feature.auth.data.network.model.response.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody

interface ProfileApiClient {
    suspend fun getMyProfile(): Result<UserDto>
    suspend fun updateProfile(updateBody: UpdateProfileRequest): Result<UserDto>

    suspend fun getUserProfile(username: String): Result<PublicUserDto>
}

class ProfileApiClientImpl(
    private val httpClient: HttpClient,
    private val authorizedRequestExecutor: AuthorizedRequestExecutor,
) : ProfileApiClient {

    override suspend fun getMyProfile(): Result<UserDto> = authorizedRequestExecutor.execute { _ ->
        httpClient.get(ApiRoutes.users.me).body<UserDto>()
    }

    override suspend fun updateProfile(updateBody: UpdateProfileRequest) =
        authorizedRequestExecutor.execute {
            httpClient.patch(ApiRoutes.users.me) {
                setBody(updateBody)
            }.body<UserDto>()
        }

    override suspend fun getUserProfile(username: String): Result<PublicUserDto> {
        return authorizedRequestExecutor.execute {
            httpClient.get(ApiRoutes.users.byUsername(username)).body<PublicUserDto>()
        }
    }
}
