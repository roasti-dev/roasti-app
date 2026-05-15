package dev.roasti.features.uploads

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface FileStorage {
  suspend fun save(id: ImageId, bytes: ByteArray)

  suspend fun load(id: ImageId): ByteArray?

  suspend fun delete(id: ImageId)
}

class LocalFileStorage(private val dir: File) : FileStorage {

  init {
    dir.mkdirs()
  }

  override suspend fun save(id: ImageId, bytes: ByteArray): Unit =
      withContext(Dispatchers.IO) { File(dir, id.value.toString()).writeBytes(bytes) }

  override suspend fun load(id: ImageId): ByteArray? =
      withContext(Dispatchers.IO) {
        val file = File(dir, id.value.toString())
        if (file.exists()) file.readBytes() else null
      }

  override suspend fun delete(id: ImageId): Unit =
      withContext(Dispatchers.IO) { File(dir, id.value.toString()).delete() }
}
