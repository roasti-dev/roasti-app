package dev.roasti.features.uploads

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

interface FileStorage {
    suspend fun save(id: String, bytes: ByteArray)
    suspend fun load(id: String): ByteArray?
}

class LocalFileStorage(private val dir: File) : FileStorage {

    init {
        dir.mkdirs()
    }

    override suspend fun save(id: String, bytes: ByteArray): Unit = withContext(Dispatchers.IO) {
        File(dir, id).writeBytes(bytes)
    }

    override suspend fun load(id: String): ByteArray? = withContext(Dispatchers.IO) {
        val file = File(dir, id)
        if (file.exists()) file.readBytes() else null
    }
}
