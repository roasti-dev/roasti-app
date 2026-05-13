package dev.roasti.features.uploads

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val MAX_SIZE_BYTES = 10 * 1024 * 1024

data class Upload(val id: String, val contentType: String, val bytes: ByteArray)

interface UploadService {
    suspend fun save(contentType: String, bytes: ByteArray): UploadMeta
    suspend fun findById(id: String): Upload?
}

@OptIn(ExperimentalUuidApi::class)
class UploadServiceImpl(
    private val repo: UploadRepository,
    private val storage: FileStorage,
) : UploadService {

    override suspend fun save(contentType: String, bytes: ByteArray): UploadMeta {
        require(bytes.isNotEmpty()) { "file must not be empty" }
        require(bytes.size <= MAX_SIZE_BYTES) { "file must not exceed 10 MB" }
        val id = Uuid.random().toString()
        storage.save(id, bytes)
        repo.save(id, contentType)
        return UploadMeta(id = id, contentType = contentType)
    }

    override suspend fun findById(id: String): Upload? {
        val meta = repo.findById(id) ?: return null
        val bytes = storage.load(id) ?: return null
        return Upload(id = id, contentType = meta.contentType, bytes = bytes)
    }
}
