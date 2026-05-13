package dev.roasti.features.likes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import dev.roasti.features.users.UserId
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class LikeInfo(val isLiked: Boolean, val count: Int) {
    companion object {
        val EMPTY = LikeInfo(isLiked = false, count = 0)
    }
}

interface LikeRepository {
    suspend fun create(userId: UserId, targetId: Uuid, targetType: LikeTargetType)
    suspend fun delete(userId: UserId, targetId: Uuid, targetType: LikeTargetType)
    suspend fun exists(userId: UserId, targetId: Uuid, targetType: LikeTargetType): Boolean
    suspend fun getInfoBatch(userId: UserId?, targetIds: List<Uuid>, targetType: LikeTargetType): List<LikeRow>
}

class LikeRepositoryImpl : LikeRepository {

    override suspend fun create(userId: UserId, targetId: Uuid, targetType: LikeTargetType): Unit =
        withContext(Dispatchers.IO) {
            transaction {
                LikeTable.insert {
                    it[LikeTable.id] = org.jetbrains.exposed.v1.core.dao.id.EntityID(Uuid.random(), LikeTable)
                    it[LikeTable.userId] = userId.value
                    it[LikeTable.targetId] = targetId
                    it[LikeTable.targetType] = targetType
                    it[LikeTable.createdAt] = Clock.System.now()
                }
            }
        }

    override suspend fun delete(userId: UserId, targetId: Uuid, targetType: LikeTargetType): Unit =
        withContext(Dispatchers.IO) {
            transaction {
                LikeTable.deleteWhere {
                    (LikeTable.userId eq userId.value) and
                        (LikeTable.targetId eq targetId) and
                        (LikeTable.targetType eq targetType)
                }
            }
        }

    override suspend fun exists(userId: UserId, targetId: Uuid, targetType: LikeTargetType): Boolean =
        withContext(Dispatchers.IO) {
            transaction {
                LikeTable.selectAll()
                    .where {
                        (LikeTable.userId eq userId.value) and
                            (LikeTable.targetId eq targetId) and
                            (LikeTable.targetType eq targetType)
                    }
                    .count() > 0
            }
        }

    override suspend fun getInfoBatch(userId: UserId?, targetIds: List<Uuid>, targetType: LikeTargetType): List<LikeRow> =
        withContext(Dispatchers.IO) {
            if (targetIds.isEmpty()) return@withContext emptyList()
            transaction {
                LikeTable.selectAll()
                    .where { (LikeTable.targetId inList targetIds) and (LikeTable.targetType eq targetType) }
                    .map { it.toLikeRow() }
            }
        }
}
