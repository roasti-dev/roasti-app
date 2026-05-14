package dev.roasti.features.uploads

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.toNonEmptyListOrNull
import dev.roasti.features.users.model.UserId
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.slf4j.LoggerFactory

private const val MAX_SIZE_BYTES = 10 * 1024 * 1024
private val UNCONFIRMED_TTL = 24.hours

sealed interface SaveUploadError {
  data object EmptyFile : SaveUploadError

  data object FileTooLarge : SaveUploadError
}

data class Upload(val id: String, val contentType: String, val bytes: ByteArray)

interface UploadService {
  suspend fun save(
      contentType: String,
      bytes: ByteArray,
      uploaderId: UserId,
  ): Either<SaveUploadError, UploadMeta>

  suspend fun findById(id: String): Upload?

  suspend fun resolveImageIds(
      ids: List<String>,
      uploaderId: UserId,
  ): EitherNel<String, List<ImageId>>

  suspend fun confirmAll(ids: List<ImageId>)

  suspend fun cleanupExpired()
}

@OptIn(ExperimentalUuidApi::class)
class UploadServiceImpl(private val repo: UploadRepository, private val storage: FileStorage) :
    UploadService {

  private val logger = LoggerFactory.getLogger(UploadServiceImpl::class.java)

  override suspend fun save(
      contentType: String,
      bytes: ByteArray,
      uploaderId: UserId,
  ): Either<SaveUploadError, UploadMeta> = either {
    ensure(bytes.isNotEmpty()) { SaveUploadError.EmptyFile }
    ensure(bytes.size <= MAX_SIZE_BYTES) { SaveUploadError.FileTooLarge }
    val id = Uuid.random().toString()
    storage.save(id, bytes)
    repo.save(id, contentType, uploaderId)
    UploadMeta(id = id, contentType = contentType)
  }

  override suspend fun findById(id: String): Upload? {
    val meta = repo.findById(id) ?: return null
    val bytes = storage.load(id) ?: return null
    return Upload(id = id, contentType = meta.contentType, bytes = bytes)
  }

  override suspend fun resolveImageIds(
      ids: List<String>,
      uploaderId: UserId,
  ): EitherNel<String, List<ImageId>> = either {
    val invalid = repo.findInvalidIds(ids, uploaderId)
    invalid.toNonEmptyListOrNull()?.let { raise(it) }
    ids.map { ImageId(it) }
  }

  override suspend fun confirmAll(ids: List<ImageId>) {
    if (ids.isEmpty()) return
    repo.confirmAll(ids.map { it.value })
  }

  override suspend fun cleanupExpired() {
    val cutoff = Clock.System.now() - UNCONFIRMED_TTL
    val ids = repo.findUnconfirmedBefore(cutoff)
    if (ids.isEmpty()) return

    ids.forEach { id ->
      try {
        storage.delete(id)
      } catch (e: Exception) {
        logger.warn("Failed to delete upload file $id from storage", e)
      }
    }
    repo.deleteAll(ids)
    logger.info("Cleaned up ${ids.size} expired unconfirmed uploads")
  }
}
