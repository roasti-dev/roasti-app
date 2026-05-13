package dev.roasti.feature.post.data.remote.model.response

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import dev.roasti.feature.post.data.remote.model.VoteDirectionDto

@Serializable
data class PostResponseDto(
    @SerialName("id")
    val id: String,
    @SerialName("author")
    val author: PostAuthorDto,
    @SerialName("title")
    val title: String? = null,
    @SerialName("text")
    val text: String = "",
    @SerialName("images")
    val images: List<String> = emptyList(),
    @SerialName("recipe")
    val recipe: PostRecipeRefDto? = null,
    @SerialName("rating")
    val rating: Int = 0,
    @SerialName("user_vote")
    val userVote: VoteDirectionDto,
    @SerialName("comments_count")
    val commentsCount: Int = 0,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("updated_at")
    val updatedAt: Instant,
)
