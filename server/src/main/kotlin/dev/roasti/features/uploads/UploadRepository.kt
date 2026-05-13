package dev.roasti.features.uploads

import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

data class UploadMeta(val id: String, val contentType: String)

interface UploadRepository {
  suspend fun save(id: String, contentType: String)

  suspend fun findById(id: String): UploadMeta?
}

@OptIn(ExperimentalUuidApi::class)
class UploadRepositoryImpl : UploadRepository {

  override suspend fun save(id: String, contentType: String): Unit =
      withContext(Dispatchers.IO) {
        val uuid = Uuid.parse(id)
        transaction {
          UploadTable.insert {
            it[UploadTable.id] = EntityID(uuid, UploadTable)
            it[UploadTable.contentType] = contentType
            it[UploadTable.createdAt] = Clock.System.now()
          }
        }
      }

  override suspend fun findById(id: String): UploadMeta? =
      withContext(Dispatchers.IO) {
        val uuid = runCatching { Uuid.parse(id) }.getOrNull() ?: return@withContext null
        transaction {
          UploadTable.selectAll()
              .where { UploadTable.id eq uuid }
              .singleOrNull()
              ?.let { UploadMeta(id = id, contentType = it[UploadTable.contentType]) }
        }
      }
}
