package dev.roasti.feature.recipe.data.remote.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecipeOriginResponseDto(
    @SerialName("author")
    val author: AuthorResponseDto,
    @SerialName("recipe_id")
    val recipeId: String,
)
