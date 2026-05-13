package dev.roasti.ui.features.postdetail.model

import androidx.compose.runtime.Immutable
import kotlinx.datetime.Instant

@Immutable
data class CommentUiModel(
    val id: String,
    val parentId: String?,
    val isDeleted: Boolean,
    val authorId: String?,
    val authorName: String?,
    val authorAvatarUrl: String?,
    val postedAt: Instant,
    val body: String,
    val isOwn: Boolean,
)

@Immutable
data class CommentThreadUiModel(
    val root: CommentUiModel,
    val replies: List<CommentUiModel>,
)
