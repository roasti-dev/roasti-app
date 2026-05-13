package dev.roasti.feature.recipe.data.remote.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import dev.roasti.feature.recipe.data.remote.model.BrewMethodDto
import dev.roasti.feature.recipe.data.remote.model.DifficultyDto
import dev.roasti.feature.recipe.data.remote.model.RoastLevelDto

@Serializable
data class CreateRecipeRequestDto(
    @SerialName("title") val title: String,
    @SerialName("beans") val beans: String? = null,
    @SerialName("brew_method") val brewMethod: BrewMethodDto,
    @SerialName("description") val description: String,
    @SerialName("note") val note: String? = null,
    @SerialName("difficulty") val difficulty: DifficultyDto,
    @SerialName("image_id") val imageId: String? = null,
    @SerialName("roast_level") val roastLevel: RoastLevelDto,
    @SerialName("steps") val steps: List<CreateRecipeStepRequestDto>,
)

@Serializable
data class CreateRecipeStepRequestDto(
    @SerialName("description") val description: String? = null,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
    @SerialName("image_id") val imageId: String? = null,
    @SerialName("order") val order: Int,
    @SerialName("title") val title: String,
)
