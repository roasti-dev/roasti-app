package dev.roasti.features.likes

import dev.roasti.features.users.model.UserId
import kotlin.uuid.Uuid


interface LikeService {
    suspend fun toggle(userId: UserId, targetId: Uuid, targetType: LikeTargetType): LikeInfo
    suspend fun getInfo(userId: UserId?, targetId: Uuid, targetType: LikeTargetType): LikeInfo
    suspend fun getInfoBatch(userId: UserId?, targetIds: List<Uuid>, targetType: LikeTargetType): Map<Uuid, LikeInfo>
}

class LikeServiceImpl(private val repo: LikeRepository) : LikeService {

    override suspend fun toggle(userId: UserId, targetId: Uuid, targetType: LikeTargetType): LikeInfo {
        val exists = repo.exists(userId, targetId, targetType)
        if (exists) {
            repo.delete(userId, targetId, targetType)
        } else {
            repo.create(userId, targetId, targetType)
        }
        return getInfo(userId, targetId, targetType)
    }

    override suspend fun getInfo(userId: UserId?, targetId: Uuid, targetType: LikeTargetType): LikeInfo =
        getInfoBatch(userId, listOf(targetId), targetType)[targetId] ?: LikeInfo.EMPTY

    override suspend fun getInfoBatch(userId: UserId?, targetIds: List<Uuid>, targetType: LikeTargetType): Map<Uuid, LikeInfo> {
        val rows = repo.getInfoBatch(userId, targetIds, targetType)
        val grouped = rows.groupBy { it.targetId }
        return targetIds.associateWith { id ->
            val group = grouped[id].orEmpty()
            LikeInfo(
                isLiked = userId != null && group.any { it.userId == userId },
                count = group.size
            )
        }
    }
}
