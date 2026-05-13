package dev.roasti.feature.upload.domain

interface UploadRepository {
    suspend fun uploadImage(fileName: String, bytes: ByteArray): Result<UploadedImage>
}
