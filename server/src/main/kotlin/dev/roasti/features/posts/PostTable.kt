package dev.roasti.features.posts

import dev.roasti.features.recipes.RecipeId
import dev.roasti.features.uploads.ImageId
import dev.roasti.features.users.UserTable
import dev.roasti.features.users.model.UserPreview
import dev.roasti.features.users.toUserPreview
import dev.roasti.features.votes.VoteInfo
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp

object PostTable : UuidTable("posts") {
  @OptIn(ExperimentalUuidApi::class)
  val authorId = reference("author_id", UserTable, onDelete = ReferenceOption.CASCADE)
  val title = varchar("title", 500).nullable()
  val text = text("text").nullable()
  val images = array<Uuid>("images")
  val recipeId = uuid("recipe_id").nullable()
  val createdAt = timestamp("created_at")
  val updatedAt = timestamp("updated_at")
}

@OptIn(ExperimentalUuidApi::class)
data class PostRow(
    val id: PostId,
    val author: UserPreview,
    val title: String?,
    val text: String?,
    val images: List<ImageId>,
    val recipeId: RecipeId?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@OptIn(ExperimentalUuidApi::class)
internal fun ResultRow.toPostRow() =
    PostRow(
        id = PostId(this[PostTable.id].value),
        author = toUserPreview(),
        title = this[PostTable.title],
        text = this[PostTable.text],
        images = this[PostTable.images].map(::ImageId),
        recipeId = this[PostTable.recipeId]?.let(::RecipeId),
        createdAt = this[PostTable.createdAt],
        updatedAt = this[PostTable.updatedAt],
    )

@OptIn(ExperimentalUuidApi::class)
internal fun PostRow.toPost(voteInfo: VoteInfo, commentsCount: Int, recipeRef: RecipeRef?) =
    Post(
        id = id,
        author = author,
        title = title,
        text = text,
        images = images,
        recipeRef = recipeRef,
        rating = voteInfo.rating,
        userVote = voteInfo.userVote,
        commentsCount = commentsCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
