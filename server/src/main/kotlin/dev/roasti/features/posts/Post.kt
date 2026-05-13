package dev.roasti.features.posts

import dev.roasti.features.users.model.UserPreview
import dev.roasti.features.votes.VoteDirection
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class) @JvmInline value class PostId(val value: Uuid)

@OptIn(ExperimentalUuidApi::class)
data class Post(
    val id: PostId,
    val author: UserPreview,
    val title: String?,
    val text: String?,
    val images: List<String>,
    // TODO: replace recipeId with recipeRef
    val recipeId: Uuid?,
    val rating: Int,
    val userVote: VoteDirection,
    val commentsCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)
