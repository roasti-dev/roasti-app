package dev.roasti.feature.likes.domain

data class RecipeLike(
    val isLiked: Boolean,
    val likeCount: Int,
)