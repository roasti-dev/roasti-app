package dev.roasti.features.comments

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import dev.roasti.common.domain.Page
import dev.roasti.features.users.UserId
import kotlin.uuid.Uuid

const val TEXT_MAX_LENGTH = 1000

interface CommentService {
    suspend fun create(userId: UserId, targetId: Uuid, targetType: CommentTargetType, text: String, parentId: CommentId?): Comment
    suspend fun update(userId: UserId, commentId: CommentId, text: String): Either<CommentErrorCode, Comment>
    suspend fun delete(userId: UserId, commentId: CommentId): Either<CommentErrorCode, Unit>
    suspend fun list(targetId: Uuid, targetType: CommentTargetType, page: Int, limit: Int): Page<CommentThread>
    suspend fun countForTarget(targetId: Uuid, targetType: CommentTargetType): Int
    suspend fun countForTargetBatch(targetIds: List<Uuid>, targetType: CommentTargetType): Map<Uuid, Int>
}

class CommentServiceImpl(private val repo: CommentRepository) : CommentService {

    override suspend fun create(
        userId: UserId,
        targetId: Uuid,
        targetType: CommentTargetType,
        text: String,
        parentId: CommentId?,
    ): Comment {
        val normalized = text.trim()
        require(normalized.isNotEmpty()) { "comment text must not be empty" }
        require(normalized.length <= TEXT_MAX_LENGTH) { "comment text must be at most $TEXT_MAX_LENGTH characters" }

        if (parentId != null) {
            require(repo.existsInTarget(parentId, targetId)) { "parent comment not found" }
        }

        return repo.create(
            CreateCommentInput(
                targetId = targetId,
                targetType = targetType,
                authorId = userId,
                text = normalized,
                parentId = parentId,
            )
        )
    }

    override suspend fun update(userId: UserId, commentId: CommentId, text: String): Either<CommentErrorCode, Comment> {
        val authorId = repo.getAuthorId(commentId) ?: return CommentErrorCode.NOT_FOUND.left()
        if (authorId != userId) return CommentErrorCode.FORBIDDEN.left()
        val normalized = text.trim()
        require(normalized.isNotEmpty()) { "comment text must not be empty" }
        require(normalized.length <= TEXT_MAX_LENGTH) { "comment text must be at most $TEXT_MAX_LENGTH characters" }
        repo.update(commentId, normalized)
        return (repo.findById(commentId) ?: return CommentErrorCode.NOT_FOUND.left()).right()
    }

    override suspend fun delete(userId: UserId, commentId: CommentId): Either<CommentErrorCode, Unit> {
        val authorId = repo.getAuthorId(commentId) ?: return CommentErrorCode.NOT_FOUND.left()
        if (authorId != userId) return CommentErrorCode.FORBIDDEN.left()
        repo.softDelete(commentId)
        return Unit.right()
    }

    override suspend fun list(targetId: Uuid, targetType: CommentTargetType, page: Int, limit: Int): Page<CommentThread> {
        val (items, total) = repo.listForTarget(targetId, targetType, page, limit)
        return Page.of(items, page, total, limit)
    }

    override suspend fun countForTarget(
        targetId: Uuid,
        targetType: CommentTargetType
    ): Int {
        return repo.countForTargetBatch(listOf(targetId), targetType)[targetId] ?: 0
    }

    override suspend fun countForTargetBatch(
        targetIds: List<Uuid>,
        targetType: CommentTargetType
    ): Map<Uuid, Int> {
        return repo.countForTargetBatch(targetIds, targetType)
    }
}
