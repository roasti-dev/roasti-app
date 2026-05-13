package dev.roasti.features.posts

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp
import dev.roasti.features.recipes.RecipeTable
import dev.roasti.features.users.UserTable
import dev.roasti.features.users.UserPreview
import dev.roasti.features.users.toUserPreview
import dev.roasti.features.votes.VoteInfo
import kotlin.uuid.ExperimentalUuidApi

object PostTable : UuidTable("posts") {
    @OptIn(ExperimentalUuidApi::class)
    val authorId = reference("author_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val title = varchar("title", 500).nullable()
    val text = text("text").nullable()
    val images = array<String>("images")
    @OptIn(ExperimentalUuidApi::class)
    val recipeId = reference("recipe_id", RecipeTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

@OptIn(ExperimentalUuidApi::class)
data class PostRow(
    val id: PostId,
    val author: UserPreview,
    val title: String?,
    val text: String?,
    val images: List<String>,
    val recipeId: kotlin.uuid.Uuid?,
    val createdAt: kotlin.time.Instant,
    val updatedAt: kotlin.time.Instant,
)

@OptIn(ExperimentalUuidApi::class)
internal fun ResultRow.toPostRow() = PostRow(
    id = PostId(this[PostTable.id].value),
    author = toUserPreview(),
    title = this[PostTable.title],
    text = this[PostTable.text],
    images = this[PostTable.images],
    recipeId = this[PostTable.recipeId]?.value,
    createdAt = this[PostTable.createdAt],
    updatedAt = this[PostTable.updatedAt],
)

@OptIn(ExperimentalUuidApi::class)
internal fun PostRow.toPost(voteInfo: VoteInfo, commentsCount: Int) = Post(
    id = id,
    author = author,
    title = title,
    text = text,
    images = images,
    recipeId = recipeId,
    rating = voteInfo.rating,
    userVote = voteInfo.userVote,
    commentsCount = commentsCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
