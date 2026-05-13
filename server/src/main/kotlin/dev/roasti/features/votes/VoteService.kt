package dev.roasti.features.votes

import dev.roasti.features.users.model.UserId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface VoteService {
    suspend fun toggle(userId: UserId, targetId: Uuid, targetType: VoteTargetType, direction: VoteDirection): VoteInfo
    suspend fun getInfo(userId: UserId?, targetId: Uuid, targetType: VoteTargetType): VoteInfo
    suspend fun getInfoBatch(userId: UserId?, targetIds: List<Uuid>, targetType: VoteTargetType): Map<Uuid, VoteInfo>
}

@OptIn(ExperimentalUuidApi::class)
class VoteServiceImpl(private val repo: VoteRepository) : VoteService {
    override suspend fun toggle(
        userId: UserId,
        targetId: Uuid,
        targetType: VoteTargetType,
        direction: VoteDirection
    ): VoteInfo {
        when (direction) {
            VoteDirection.UP, VoteDirection.DOWN -> repo.upsert(
                userId,
                targetId,
                targetType,
                direction
            )

            VoteDirection.NONE -> {
                repo.delete(userId, targetId, targetType)

            }
        }
        return getInfo(userId, targetId, targetType)
    }

    override suspend fun getInfo(userId: UserId?, targetId: Uuid, targetType: VoteTargetType): VoteInfo =
        getInfoBatch(userId, listOf(targetId), targetType)[targetId]
            ?: VoteInfo.EMPTY

    override suspend fun getInfoBatch(userId: UserId?, targetIds: List<Uuid>, targetType: VoteTargetType): Map<Uuid, VoteInfo> {

        // TODO: replace it with fetchRatings + fetchUserVotes

        val rows = repo.getLikes(userId, targetIds, targetType)

        val groupedByTarget = rows.groupBy { it.targetId }
        val groupedByUser = rows.groupBy { it.targetId }
            .mapValues { (_, group) ->
                group.associateBy { it.userId }
            }

        return targetIds.associateWith { targetId ->
            val group = groupedByTarget[targetId] ?: emptyList()
            val userIndex = groupedByUser[targetId].orEmpty()

            val rating = group.sumOf { it.voteType.score }

            val userVote = userId?.let { id ->
                userIndex[id]?.voteType
            } ?: VoteDirection.NONE

            VoteInfo(rating, userVote)
        }
    }
}
