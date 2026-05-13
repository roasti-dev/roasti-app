package dev.roasti.feature.upload.data

import dev.roasti.feature.upload.data.mapper.toDomain
import dev.roasti.feature.upload.data.network.UploadApiClient
import dev.roasti.feature.upload.domain.UploadRepository
import dev.roasti.feature.upload.domain.UploadedImage

class NetworkUploadRepository(
    private val apiClient: UploadApiClient,
) : UploadRepository {

    override suspend fun uploadImage(fileName: String, bytes: ByteArray): Result<UploadedImage> =
        apiClient.uploadImage(fileName, bytes).map { it.toDomain() }
}
