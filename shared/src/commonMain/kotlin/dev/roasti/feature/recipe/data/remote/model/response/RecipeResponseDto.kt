package dev.roasti.feature.recipe.data.remote.model.response

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import dev.roasti.feature.recipe.data.remote.model.BrewMethodDto
import dev.roasti.feature.recipe.data.remote.model.DifficultyDto
import dev.roasti.feature.recipe.data.remote.model.RoastLevelDto

@Serializable
data class RecipeResponseDto(
    @SerialName("id")
    val id: String,
    @SerialName("author_id")
    val authorId: String,
    @SerialName("author")
    val author: AuthorResponseDto? = null,
    @SerialName("title")
    val title: String,
    @SerialName("description")
    val description: String,
    @SerialName("note")
    val note: String? = null,
    @SerialName("image_id")
    val imageId: String? = null,
    @SerialName("brew_method")
    val brewMethod: BrewMethodDto? = null,
    @SerialName("difficulty")
    val difficulty: DifficultyDto,
    @SerialName("roast_level")
    val roastLevel: RoastLevelDto? = null,
    @SerialName("beans")
    val beans: String? = null,
    @SerialName("steps")
    val steps: List<RecipeStepResponseDto>? = null,
    @SerialName("is_liked")
    val isLiked: Boolean = false,
    @SerialName("likes_count")
    val likesCount: Int = 0,
    @SerialName("origin")
    val origin: RecipeOriginResponseDto? = null,
    @SerialName("public")
    val isPublic: Boolean = true,
    @SerialName("created_at")
    val createdAt: Instant? = null,
    @SerialName("updated_at")
    val updatedAt: Instant? = null,
)
