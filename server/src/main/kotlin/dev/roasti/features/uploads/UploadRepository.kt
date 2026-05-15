package dev.roasti.features.uploads

import dev.roasti.features.users.UserTable
import dev.roasti.features.users.model.UserId
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

data class UploadMeta(val id: ImageId, val contentType: String)

interface UploadRepository {
  suspend fun save(id: ImageId, contentType: String, uploaderId: UserId)

  suspend fun findById(id: ImageId): UploadMeta?

  suspend fun findInvalidIds(ids: List<ImageId>, uploaderId: UserId): List<ImageId>

  suspend fun confirmAll(ids: List<ImageId>)

  suspend fun findUnconfirmedBefore(cutoff: Instant): List<ImageId>

  suspend fun deleteAll(ids: List<ImageId>)
}

@OptIn(ExperimentalUuidApi::class)
class UploadRepositoryImpl : UploadRepository {

  override suspend fun save(id: ImageId, contentType: String, uploaderId: UserId): Unit =
      withContext(Dispatchers.IO) {
        transaction {
          UploadTable.insert {
            it[UploadTable.id] = EntityID(id.value, UploadTable)
            it[UploadTable.contentType] = contentType
            it[UploadTable.uploaderId] = EntityID(uploaderId.value, UserTable)
            it[UploadTable.createdAt] = Clock.System.now()
          }
        }
      }

  override suspend fun findById(id: ImageId): UploadMeta? =
      withContext(Dispatchers.IO) {
        transaction {
          UploadTable.selectAll()
              .where { UploadTable.id eq id.value }
              .singleOrNull()
              ?.let { UploadMeta(id = id, contentType = it[UploadTable.contentType]) }
        }
      }

  override suspend fun findInvalidIds(ids: List<ImageId>, uploaderId: UserId): List<ImageId> =
      withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()
        val uuids = ids.map { it.value }
        val validUuids = transaction {
          UploadTable.selectAll()
              .where {
                (UploadTable.id inList uuids) and
                    (UploadTable.uploaderId eq EntityID(uploaderId.value, UserTable))
              }
              .map { it[UploadTable.id].value }
              .toSet()
        }
        ids.filter { it.value !in validUuids }
      }

  override suspend fun confirmAll(ids: List<ImageId>): Unit =
      withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        transaction {
          UploadTable.update({ UploadTable.id inList ids.map { it.value } }) {
            it[confirmed] = true
          }
        }
      }

  override suspend fun findUnconfirmedBefore(cutoff: Instant): List<ImageId> =
      withContext(Dispatchers.IO) {
        transaction {
          UploadTable.selectAll()
              .where { (UploadTable.confirmed eq false) and (UploadTable.createdAt less cutoff) }
              .map { ImageId(it[UploadTable.id].value) }
        }
      }

  override suspend fun deleteAll(ids: List<ImageId>): Unit =
      withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        transaction { UploadTable.deleteWhere { UploadTable.id inList ids.map { it.value } } }
      }
}
