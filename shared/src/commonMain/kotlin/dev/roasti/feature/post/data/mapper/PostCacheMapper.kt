package dev.roasti.feature.post.data.mapper

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import dev.roasti.RoastiDatabaseCache
import dev.roasti.feature.post.data.remote.model.response.PostResponseDto

private val imagesJson = Json { ignoreUnknownKeys = true }
private val imagesSerializer = ListSerializer(String.serializer())

fun List<String>.encodeImages(): String =
    imagesJson.encodeToString(imagesSerializer, this)

fun String.parseImages(): List<String> =
    if (isBlank()) emptyList() else imagesJson.decodeFromString(imagesSerializer, this)

fun RoastiDatabaseCache.upsertPost(dto: PostResponseDto) {
    postQueries.insertPost(
        id = dto.id,
        title = dto.title,
        text = dto.text,
        images_json = dto.images.encodeImages(),
        recipe_id = dto.recipe?.id,
        recipe_status = dto.recipe?.status?.toDomain()?.toWireString(),
        rating = dto.rating.toLong(),
        user_vote = dto.userVote.toDomain().toWireString(),
        comments_count = dto.commentsCount.toLong(),
        author_id = dto.author.id,
        author_name = dto.author.username,
        author_image_id = dto.author.avatarId,
        created_at = dto.createdAt.toString(),
        updated_at = dto.updatedAt.toString(),
    )
}
