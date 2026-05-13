package dev.roasti.features.votes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.Case
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.case
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import dev.roasti.features.users.User
import dev.roasti.features.users.UserId
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class VoteDirection(val score: Int) { UP(1), DOWN(-1), NONE(0) }

enum class VoteTargetType { POST }

data class VoteInfo(val rating: Int, val userVote: VoteDirection) {
    companion object {
        val EMPTY = VoteInfo(0, VoteDirection.NONE)
    }
}

@OptIn(ExperimentalUuidApi::class)
interface VoteRepository {
    suspend fun upsert(userId: UserId, targetId: Uuid, targetType: VoteTargetType, direction: VoteDirection)
    suspend fun delete(userId: UserId, targetId: Uuid, targetType: VoteTargetType)
    suspend fun getVotes(userId: UserId?, targetIds: List<Uuid>, targetType: VoteTargetType): List<VoteRow>
    suspend fun fetchRatings(targetIds: List<Uuid>): Map<Uuid, Int>
    suspend fun fetchUserVotes(userId: UserId, targetIds: List<Uuid>): Map<Uuid, VoteDirection>
}

@OptIn(ExperimentalUuidApi::class)
class VoteRepositoryImpl : VoteRepository {

    override suspend fun upsert(userId: UserId, targetId: Uuid, targetType: VoteTargetType, direction: VoteDirection): Unit =
        withContext(Dispatchers.IO) {
            transaction {
                VoteTable.upsert {
                    it[VoteTable.userId] = userId.value
                    it[VoteTable.targetId] = targetId
                    it[VoteTable.targetType] = targetType
                    it[VoteTable.voteType] = direction
                    it[VoteTable.createdAt] = Clock.System.now()
                }
            }
        }

    override suspend fun delete(userId: UserId, targetId: Uuid, targetType: VoteTargetType): Unit =
        withContext(Dispatchers.IO) {
            transaction {
                VoteTable.deleteWhere {
                    (VoteTable.userId eq userId.value) and
                        (VoteTable.targetId eq targetId) and
                        (VoteTable.targetType eq targetType)
                }
            }
        }

    override suspend fun getVotes(userId: UserId?, targetIds: List<Uuid>, targetType: VoteTargetType): List<VoteRow> =
        withContext(Dispatchers.IO) {
            if (targetIds.isEmpty()) return@withContext emptyList()
            transaction {
                VoteTable.selectAll()
                    .where { (VoteTable.targetId inList targetIds) and (VoteTable.targetType eq targetType) }
                    .map { it.toVoteRow() }
            }
        }

    override suspend fun fetchRatings(targetIds: List<Uuid>): Map<Uuid, Int> =
        TODO("not implemented")
//        withContext(Dispatchers.IO) {
//            transaction {
//                val scoreExpr = Expression.build {
//                    case()
//                        .When(VoteTable.voteType eq VoteDirection.UP, 1)
//                        .When(VoteTable.voteType eq VoteDirection.DOWN, -1)
//                        .Else(0)
//
//                }
//
//                VoteTable
//                    .select(VoteTable.targetId, scoreExpr.sum().alias("rating"))
//                    .where(VoteTable.targetId inList targetIds)
//                    .groupBy(VoteTable.targetId)
//                    .associate { it[VoteTable.targetId] to it[scoreExpr.sum().alias("rating")] }
//            }
//        }


    override suspend fun fetchUserVotes(
        userId: UserId,
        targetIds: List<Uuid>
    ): Map<Uuid, VoteDirection> =
        withContext(Dispatchers.IO) {
            transaction {
                VoteTable
                    .select(VoteTable.targetId, VoteTable.voteType)
                    .where {
                        (VoteTable.targetId inList targetIds) and
                                (VoteTable.userId eq userId.value)
                    }
                    .associate {
                        it[VoteTable.targetId] to it[VoteTable.voteType]
                    }
            }
        }

}
