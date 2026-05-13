package dev.roasti.features.likes

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp
import dev.roasti.features.users.UserId
import dev.roasti.features.users.UserTable
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class LikeTargetType { RECIPE }

// TODO: consider deleting the id and using a combination of fields
//  as the key - user_id, target_id, target_type
@OptIn(ExperimentalUuidApi::class)
object LikeTable : UuidTable("likes") {
    val userId = reference("user_id", UserTable)
    val targetId = uuid("target_id")
    val targetType = enumerationByName<LikeTargetType>("target_type", 50)
    val createdAt = timestamp("created_at")

    init {
        uniqueIndex(userId, targetId, targetType)
    }
}

data class LikeRow(
    val userId: UserId,
    val targetId: Uuid,
    val targetType: LikeTargetType,
    val createdAt: Instant
)

fun ResultRow.toLikeRow() = LikeRow(
    userId = UserId(this[LikeTable.userId].value),
    targetId = this[LikeTable.targetId],
    targetType = this[LikeTable.targetType],
    createdAt = this[LikeTable.createdAt]
)