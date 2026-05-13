package dev.roasti.feature.comment.data.mapper

import kotlinx.datetime.Instant
import dev.roasti.CommentEntity
import dev.roasti.RoastiDatabaseCache
import dev.roasti.feature.comment.data.remote.model.response.CommentResponseDto
import dev.roasti.feature.comment.data.remote.model.response.CommentThreadResponseDto
import dev.roasti.feature.comment.domain.model.Comment
import dev.roasti.feature.comment.domain.model.CommentAuthor

fun CommentEntity.toDomain(): Comment {
    val authorIdValue = author_id
    val authorUsernameValue = author_username
    val author = if (is_deleted == 0L && authorIdValue != null && authorUsernameValue != null) {
        CommentAuthor(
            id = authorIdValue,
            username = authorUsernameValue,
            avatarId = author_avatar_id,
        )
    } else {
        null
    }
    return Comment(
        id = id,
        parentId = parent_id,
        isDeleted = is_deleted == 1L,
        author = author,
        text = text,
        createdAt = Instant.parse(created_at),
        updatedAt = Instant.parse(updated_at),
    )
}

fun RoastiDatabaseCache.upsertCommentThread(
    postId: String,
    thread: CommentThreadResponseDto,
    rootPosition: Int,
) {
    commentEntityQueries.upsertComment(
        id = thread.id,
        post_id = postId,
        parent_id = null,
        is_deleted = if (thread.isDeleted) 1L else 0L,
        author_id = thread.author?.id,
        author_username = thread.author?.username,
        author_avatar_id = thread.author?.avatarId,
        text = thread.text,
        created_at = thread.createdAt.toString(),
        updated_at = thread.updatedAt.toString(),
        position = rootPosition.toLong(),
    )
    thread.replies.forEachIndexed { replyIndex, reply ->
        upsertReply(postId = postId, parentId = thread.id, dto = reply, position = replyIndex)
    }
}

private fun RoastiDatabaseCache.upsertReply(
    postId: String,
    parentId: String,
    dto: CommentResponseDto,
    position: Int,
) {
    commentEntityQueries.upsertComment(
        id = dto.id,
        post_id = postId,
        parent_id = parentId,
        is_deleted = if (dto.isDeleted) 1L else 0L,
        author_id = dto.author?.id,
        author_username = dto.author?.username,
        author_avatar_id = dto.author?.avatarId,
        text = dto.text,
        created_at = dto.createdAt.toString(),
        updated_at = dto.updatedAt.toString(),
        position = position.toLong(),
    )
}
