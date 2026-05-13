package dev.roasti.features.comments

import dev.roasti.features.users.UserPreview
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@JvmInline
value class CommentId(val value: Uuid)

@OptIn(ExperimentalUuidApi::class)
sealed class Comment {
    abstract val id: CommentId
    abstract val parentId: CommentId?
    abstract val createdAt: Instant
    abstract val updatedAt: Instant

    data class Active(
        override val id: CommentId,
        override val parentId: CommentId?,
        override val createdAt: Instant,
        override val updatedAt: Instant,
        val author: UserPreview,
        val text: String,
    ) : Comment()

    data class Deleted(
        override val id: CommentId,
        override val parentId: CommentId?,
        override val createdAt: Instant,
        override val updatedAt: Instant,
    ) : Comment()
}

data class CommentThread(
    val root: Comment,
    val replies: List<Comment>,
)
