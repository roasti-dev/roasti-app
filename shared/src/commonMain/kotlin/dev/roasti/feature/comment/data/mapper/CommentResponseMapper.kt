package dev.roasti.feature.comment.data.mapper

import dev.roasti.feature.comment.data.remote.model.response.CommentAuthorDto
import dev.roasti.feature.comment.data.remote.model.response.CommentResponseDto
import dev.roasti.feature.comment.data.remote.model.response.CommentThreadResponseDto
import dev.roasti.feature.comment.domain.model.Comment
import dev.roasti.feature.comment.domain.model.CommentAuthor
import dev.roasti.feature.comment.domain.model.CommentThread

fun CommentAuthorDto.toDomain(): CommentAuthor = CommentAuthor(
    id = id,
    username = username,
    avatarId = avatarId,
)

fun CommentResponseDto.toDomain(): Comment = Comment(
    id = id,
    parentId = parentId,
    isDeleted = isDeleted,
    author = author?.toDomain(),
    text = text,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun CommentThreadResponseDto.toDomain(): CommentThread = CommentThread(
    root = Comment(
        id = id,
        parentId = parentId,
        isDeleted = isDeleted,
        author = author?.toDomain(),
        text = text,
        createdAt = createdAt,
        updatedAt = updatedAt,
    ),
    replies = replies.map { it.toDomain() },
)
