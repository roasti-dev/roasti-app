package dev.roasti.features.recipes

import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.RoastLevel
import dev.roasti.features.users.model.UserPreview
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@JvmInline @OptIn(ExperimentalUuidApi::class) value class RecipeId(val value: Uuid)

data class RecipeOriginInfo(val author: UserPreview, val recipeId: RecipeId)

@OptIn(ExperimentalUuidApi::class)
data class Recipe(
    val id: RecipeId,
    val author: UserPreview,
    val title: String,
    val description: String,
    val note: String?,
    val imageId: String?,
    val brewMethod: BrewMethod,
    val difficulty: Difficulty,
    val roastLevel: RoastLevel,
    val beans: String?,
    val public: Boolean,
    val steps: List<BrewStep>,
    val isLiked: Boolean,
    val likesCount: Int,
    val origin: RecipeOriginInfo?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
