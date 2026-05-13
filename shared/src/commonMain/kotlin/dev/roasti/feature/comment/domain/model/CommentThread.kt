package dev.roasti.feature.comment.domain.model

data class CommentThread(
    val root: Comment,
    val replies: List<Comment>,
)
