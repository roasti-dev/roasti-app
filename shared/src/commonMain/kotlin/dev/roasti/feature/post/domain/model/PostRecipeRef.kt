package dev.roasti.feature.post.domain.model

data class PostRecipeRef(
    val id: String,
    val status: PostRecipeStatus,
)

enum class PostRecipeStatus {
    AVAILABLE,
    UNAVAILABLE,
}
