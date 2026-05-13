package dev.roasti.feature.likes.domain

import dev.roasti.feature.recipe.domain.model.Recipe

data class LikedRecipe(
    val likedAt: String,
    val recipe: Recipe,
)

