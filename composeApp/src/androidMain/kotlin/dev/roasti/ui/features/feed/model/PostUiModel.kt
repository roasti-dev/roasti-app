package dev.roasti.ui.features.feed.model

import androidx.compose.runtime.Immutable
import kotlinx.datetime.Instant
import dev.roasti.ui.uikit.post.PostRatingStateUi

@Immutable
data class PostUiModel(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorImageUrl: String?,
    val postedAt: Instant,
    val title: String?,
    val body: String?,
    val postImageUrl: String?,
    val ratingState: PostRatingStateUi,
    val commentsCount: Int,
    val isOwn: Boolean,
)
