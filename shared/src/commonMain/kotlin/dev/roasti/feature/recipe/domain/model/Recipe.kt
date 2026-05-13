package dev.roasti.feature.recipe.domain.model

import kotlinx.datetime.Instant

data class Recipe(
    val id: String,
    val title: String,
    val description: String,
    val note: String? = null,
    val imageId: String?,
    val brewMethod: BrewMethod,
    val difficulty: Difficulty,
    val roastLevel: RoastLevel,
    val beans: String?,
    val steps: List<BrewStep>,
    val author: Author?,
    val isLiked: Boolean,
    val likesCount: Int,
    val origin: RecipeOrigin?,
    val isPublic: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

