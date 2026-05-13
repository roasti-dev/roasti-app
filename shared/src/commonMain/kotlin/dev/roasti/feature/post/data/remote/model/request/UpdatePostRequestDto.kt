package dev.roasti.feature.post.data.remote.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdatePostRequestDto(
    @SerialName("title")
    val title: String? = null,
    @SerialName("text")
    val text: String? = null,
    @SerialName("images")
    val images: List<String> = emptyList(),
    @SerialName("recipe_id")
    val recipeId: String? = null,
)
