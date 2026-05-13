package dev.roasti.features.votes

import dev.roasti.features.users.UserTable
import dev.roasti.features.users.model.UserId
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

@OptIn(ExperimentalUuidApi::class)
object VoteTable : Table("votes") {
  val userId = reference("user_id", UserTable)
  val targetId = uuid("target_id")
  val targetType = enumerationByName<VoteTargetType>("target_type", 50)
  val voteType = enumerationByName<VoteDirection>("vote_type", 10)
  val createdAt = timestamp("created_at")

  override val primaryKey = PrimaryKey(userId, targetId, targetType)
}

data class VoteRow(
    val userId: UserId,
    val targetId: Uuid,
    val targetType: VoteTargetType,
    val voteType: VoteDirection,
    val createdAt: Instant,
)

fun ResultRow.toVoteRow() =
    VoteRow(
        userId = UserId(this[VoteTable.userId].value),
        targetId = this[VoteTable.targetId],
        targetType = this[VoteTable.targetType],
        voteType = this[VoteTable.voteType],
        createdAt = this[VoteTable.createdAt],
    )
