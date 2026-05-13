package dev.roasti.feature.upload.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import dev.roasti.core.network.ApiRoutes
import dev.roasti.core.network.AuthorizedRequestExecutor
import dev.roasti.feature.upload.data.remote.model.response.ImageUploadResponseDto

interface UploadApiClient {
    suspend fun uploadImage(fileName: String, bytes: ByteArray): Result<ImageUploadResponseDto>
}

class UploadApiClientImpl(
    private val httpClient: HttpClient,
    private val authorizedRequestExecutor: AuthorizedRequestExecutor,
) : UploadApiClient {

    override suspend fun uploadImage(fileName: String, bytes: ByteArray): Result<ImageUploadResponseDto> =
        authorizedRequestExecutor.execute { _ ->
            httpClient.post(ApiRoutes.UploadsImages) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("file", bytes, Headers.build {
                                append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                            })
                        }
                    )
                )
            }.body<ImageUploadResponseDto>()
        }
}
