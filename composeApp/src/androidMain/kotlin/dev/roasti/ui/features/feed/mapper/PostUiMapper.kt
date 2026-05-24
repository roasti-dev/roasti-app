package dev.roasti.ui.features.feed.mapper

import dev.roasti.core.utils.imageUrl
import dev.roasti.feature.post.domain.model.Post
import dev.roasti.feature.post.domain.model.VoteDirection
import dev.roasti.ui.features.feed.model.PostUiModel
import dev.roasti.ui.uikit.post.PostRatingStateUi
import dev.roasti.ui.uikit.post.PostUserReaction

fun Post.toUiModel(currentUserId: String? = null): PostUiModel = PostUiModel(
    id = id,
    authorId = author.id,
    authorName = author.name,
    authorImageUrl = author.imageId?.let(::imageUrl),
    postedAt = createdAt,
    title = title?.takeIf { it.isNotBlank() },
    body = text.takeIf { it.isNotBlank() },
    images = images.map(::imageUrl),
    ratingState = PostRatingStateUi(
        userReaction = userVote.toUi(),
        postRating = rating,
    ),
    commentsCount = commentsCount,
    isOwn = currentUserId != null && author.id == currentUserId,
)

fun VoteDirection.toUi(): PostUserReaction = when (this) {
    VoteDirection.UP -> PostUserReaction.UP
    VoteDirection.DOWN -> PostUserReaction.DOWN
    VoteDirection.NONE -> PostUserReaction.NONE
}

fun PostUserReaction.toDomain(): VoteDirection = when (this) {
    PostUserReaction.UP -> VoteDirection.UP
    PostUserReaction.DOWN -> VoteDirection.DOWN
    PostUserReaction.NONE -> VoteDirection.NONE
}
