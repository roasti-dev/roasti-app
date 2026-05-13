package dev.roasti.feature.post.data.mapper

import kotlinx.datetime.Instant
import dev.roasti.Post as CachedPost
import dev.roasti.feature.post.data.remote.model.response.PostResponseDto
import dev.roasti.feature.post.domain.model.Post
import dev.roasti.feature.post.domain.model.PostAuthor
import dev.roasti.feature.post.domain.model.PostRecipeRef

fun PostResponseDto.toDomain(): Post = Post(
    id = id,
    title = title,
    text = text,
    images = images,
    recipe = recipe?.toDomain(),
    rating = rating,
    userVote = userVote.toDomain(),
    commentsCount = commentsCount,
    author = PostAuthor(
        id = author.id,
        name = author.username,
        imageId = author.avatarId,
    ),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun CachedPost.toDomain(): Post = Post(
    id = id,
    title = title,
    text = text,
    images = images_json.parseImages(),
    recipe = recipe_id?.let { id ->
        PostRecipeRef(id = id, status = recipe_status.toPostRecipeStatus())
    },
    rating = rating.toInt(),
    userVote = user_vote.toVoteDirection(),
    commentsCount = comments_count.toInt(),
    author = PostAuthor(
        id = author_id,
        name = author_name,
        imageId = author_image_id,
    ),
    createdAt = Instant.parse(created_at),
    updatedAt = Instant.parse(updated_at),
)
