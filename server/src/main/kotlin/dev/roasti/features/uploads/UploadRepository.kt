package dev.roasti.features.uploads

import dev.roasti.features.users.UserTable
import dev.roasti.features.users.model.UserId
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
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

data class UploadMeta(val id: String, val contentType: String)

interface UploadRepository {
  suspend fun save(id: String, contentType: String, uploaderId: UserId)

  suspend fun findById(id: String): UploadMeta?

  suspend fun findInvalidIds(ids: List<String>, uploaderId: UserId): List<String>

  suspend fun confirmAll(ids: List<String>)

  suspend fun findUnconfirmedBefore(cutoff: Instant): List<String>

  suspend fun deleteAll(ids: List<String>)
}

@OptIn(ExperimentalUuidApi::class)
class UploadRepositoryImpl : UploadRepository {

  override suspend fun save(id: String, contentType: String, uploaderId: UserId): Unit =
      withContext(Dispatchers.IO) {
        val uuid = Uuid.parse(id)
        transaction {
          UploadTable.insert {
            it[UploadTable.id] = EntityID(uuid, UploadTable)
            it[UploadTable.contentType] = contentType
            it[UploadTable.uploaderId] = EntityID(uploaderId.value, UserTable)
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

  override suspend fun findInvalidIds(ids: List<String>, uploaderId: UserId): List<String> =
      withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()
        val uuids = ids.mapNotNull { runCatching { Uuid.parse(it) }.getOrNull() }
        val validIds = transaction {
          UploadTable.selectAll()
              .where {
                (UploadTable.id inList uuids) and
                    (UploadTable.uploaderId eq EntityID(uploaderId.value, UserTable))
              }
              .map { it[UploadTable.id].value.toString() }
              .toSet()
        }
        ids.filter { it !in validIds }
      }

  override suspend fun confirmAll(ids: List<String>): Unit =
      withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val uuids = ids.mapNotNull { runCatching { Uuid.parse(it) }.getOrNull() }
        transaction { UploadTable.update({ UploadTable.id inList uuids }) { it[confirmed] = true } }
      }

  override suspend fun findUnconfirmedBefore(cutoff: Instant): List<String> =
      withContext(Dispatchers.IO) {
        transaction {
          UploadTable.selectAll()
              .where { (UploadTable.confirmed eq false) and (UploadTable.createdAt less cutoff) }
              .map { it[UploadTable.id].value.toString() }
        }
      }

  override suspend fun deleteAll(ids: List<String>): Unit =
      withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val uuids = ids.mapNotNull { runCatching { Uuid.parse(it) }.getOrNull() }
        transaction { UploadTable.deleteWhere { UploadTable.id inList uuids } }
      }
}
