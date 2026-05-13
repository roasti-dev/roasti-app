package dev.roasti.feature.likes.data

import dev.roasti.feature.recipe.data.mapper.toDomain
import dev.roasti.feature.likes.domain.RecipeLike
import dev.roasti.feature.likes.domain.LikedRecipe
import dev.roasti.core.domain.Page
import dev.roasti.core.network.PageResponseDto

fun RecipeLikeDto.toDomain() = RecipeLike(isLiked, likesCount)

fun PageResponseDto<LikedRecipeItemDto>.toDomain() = Page(
    items = items.map { it.toDomain() },
    currentPage = pagination.currentPage,
    itemsCount = pagination.itemsCount,
    lastPage = pagination.lastPage,
    nextPage = pagination.nextPage,
)

fun LikedRecipeItemDto.toDomain() = LikedRecipe(
    likedAt = likedAt,
    recipe = recipe.toDomain(),
)
