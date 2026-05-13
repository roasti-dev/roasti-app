package dev.roasti.feature.recipe.data.remote.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecipeStepResponseDto(
    @SerialName("order")
    val order: Int,
    @SerialName("title")
    val title: String,
    @SerialName("description")
    val description: String,
    @SerialName("duration_seconds")
    val durationSeconds: Int? = null,
    @SerialName("image_id")
    val imageId: String? = null,
)
