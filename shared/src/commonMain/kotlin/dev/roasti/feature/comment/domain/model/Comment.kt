package dev.roasti.feature.comment.domain.model

import kotlinx.datetime.Instant

data class Comment(
    val id: String,
    val parentId: String?,
    val isDeleted: Boolean,
    val author: CommentAuthor?,
    val text: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
