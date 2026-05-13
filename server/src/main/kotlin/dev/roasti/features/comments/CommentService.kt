package dev.roasti.features.comments

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import dev.roasti.common.domain.Page
import dev.roasti.features.users.model.UserId
import kotlin.uuid.Uuid

const val TEXT_MAX_LENGTH = 1000

sealed interface CreateCommentError {
  data class InvalidInput(val message: String) : CreateCommentError
}

sealed interface UpdateCommentError {
  data object NotFound : UpdateCommentError

  data object Forbidden : UpdateCommentError

  data class InvalidInput(val message: String) : UpdateCommentError
}

sealed interface DeleteCommentError {
  data object NotFound : DeleteCommentError

  data object Forbidden : DeleteCommentError
}

interface CommentService {
  suspend fun create(
      userId: UserId,
      targetId: Uuid,
      targetType: CommentTargetType,
      text: String,
      parentId: CommentId?,
  ): Either<CreateCommentError, Comment>

  suspend fun update(
      userId: UserId,
      commentId: CommentId,
      text: String,
  ): Either<UpdateCommentError, Comment>

  suspend fun delete(userId: UserId, commentId: CommentId): Either<DeleteCommentError, Unit>

  suspend fun list(
      targetId: Uuid,
      targetType: CommentTargetType,
      page: Int,
      limit: Int,
  ): Page<CommentThread>

  suspend fun countForTarget(targetId: Uuid, targetType: CommentTargetType): Int

  suspend fun countForTargetBatch(
      targetIds: List<Uuid>,
      targetType: CommentTargetType,
  ): Map<Uuid, Int>
}

class CommentServiceImpl(private val repo: CommentRepository) : CommentService {

  override suspend fun create(
      userId: UserId,
      targetId: Uuid,
      targetType: CommentTargetType,
      text: String,
      parentId: CommentId?,
  ): Either<CreateCommentError, Comment> = either {
    val normalized = text.trim()
    if (normalized.isBlank())
        raise(CreateCommentError.InvalidInput("comment text must not be empty"))
    if (normalized.length > TEXT_MAX_LENGTH)
        raise(
            CreateCommentError.InvalidInput(
                "comment text must be at most $TEXT_MAX_LENGTH characters"
            )
        )

    if (parentId != null) {
      if (!repo.existsInTarget(parentId, targetId))
          raise(CreateCommentError.InvalidInput("parent comment not found"))
    }

    repo.create(
        CreateCommentInput(
            targetId = targetId,
            targetType = targetType,
            authorId = userId,
            text = normalized,
            parentId = parentId,
        )
    )
  }

  override suspend fun update(
      userId: UserId,
      commentId: CommentId,
      text: String,
  ): Either<UpdateCommentError, Comment> = either {
    val authorId = repo.getAuthorId(commentId) ?: raise(UpdateCommentError.NotFound)
    if (authorId != userId) raise(UpdateCommentError.Forbidden)
    val normalized = text.trim()
    ensure(normalized.isNotBlank()) {
      UpdateCommentError.InvalidInput("comment text must not be empty")
    }
    ensure(normalized.length <= TEXT_MAX_LENGTH) {
      UpdateCommentError.InvalidInput("comment text must be at most $TEXT_MAX_LENGTH characters")
    }
    // TODO: return comment from update and handle error
    repo.update(commentId, normalized)
    repo.findById(commentId)!!
  }

  override suspend fun delete(
      userId: UserId,
      commentId: CommentId,
  ): Either<DeleteCommentError, Unit> = either {
    val authorId = repo.getAuthorId(commentId) ?: raise(DeleteCommentError.NotFound)
    ensure(authorId != userId) { DeleteCommentError.Forbidden }
    repo.softDelete(commentId)
  }

  override suspend fun list(
      targetId: Uuid,
      targetType: CommentTargetType,
      page: Int,
      limit: Int,
  ): Page<CommentThread> {
    val (items, total) = repo.listForTarget(targetId, targetType, page, limit)
    return Page.of(items, page, total, limit)
  }

  override suspend fun countForTarget(targetId: Uuid, targetType: CommentTargetType): Int {
    return repo.countForTargetBatch(listOf(targetId), targetType)[targetId] ?: 0
  }

  override suspend fun countForTargetBatch(
      targetIds: List<Uuid>,
      targetType: CommentTargetType,
  ): Map<Uuid, Int> {
    return repo.countForTargetBatch(targetIds, targetType)
  }
}
