package dev.roasti.feature.post.data.remote.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostRecipeRefDto(
    @SerialName("id")
    val id: String,
    @SerialName("status")
    val status: PostRecipeStatusDto,
)

@Serializable
enum class PostRecipeStatusDto {
    @SerialName("available") AVAILABLE,
    @SerialName("unavailable") UNAVAILABLE,
}
