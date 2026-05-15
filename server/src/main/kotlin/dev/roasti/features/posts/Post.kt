package dev.roasti.features.posts

import dev.roasti.features.recipes.RecipeId
import dev.roasti.features.uploads.ImageId
import dev.roasti.features.users.model.UserPreview
import dev.roasti.features.votes.VoteDirection
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@OptIn(ExperimentalUuidApi::class) @JvmInline @Serializable value class PostId(val value: Uuid)

@OptIn(ExperimentalUuidApi::class)
sealed interface RecipeRef {
  val id: RecipeId

  data class Available(override val id: RecipeId) : RecipeRef

  data class Unavailable(override val id: RecipeId) : RecipeRef
}

@OptIn(ExperimentalUuidApi::class)
data class Post(
    val id: PostId,
    val author: UserPreview,
    val title: String?,
    val text: String?,
    val images: List<ImageId>,
    val recipeRef: RecipeRef?,
    val rating: Int,
    val userVote: VoteDirection,
    val commentsCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)
