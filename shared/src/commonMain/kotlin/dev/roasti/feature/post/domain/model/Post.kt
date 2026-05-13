package dev.roasti.feature.post.domain.model

import kotlinx.datetime.Instant

data class Post(
    val id: String,
    val title: String?,
    val text: String,
    val images: List<String>,
    val recipe: PostRecipeRef?,
    val rating: Int,
    val userVote: VoteDirection,
    val commentsCount: Int,
    val author: PostAuthor,
    val createdAt: Instant,
    val updatedAt: Instant,
)
