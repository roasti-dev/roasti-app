package dev.roasti.testing

import dev.roasti.core.network.PageResponseDto
import dev.roasti.core.network.PaginationResponseDto
import dev.roasti.feature.likes.data.LikedRecipeItemDto
import dev.roasti.feature.recipe.data.remote.model.BrewMethodDto
import dev.roasti.feature.recipe.data.remote.model.DifficultyDto
import dev.roasti.feature.recipe.data.remote.model.RoastLevelDto
import dev.roasti.feature.recipe.data.remote.model.response.RecipeResponseDto

object RecipeFixtures {
    fun dto(
        id: String = "r1",
        title: String = "Title $id",
        description: String = "Desc",
        isLiked: Boolean = false,
        likesCount: Int = 0,
    ): RecipeResponseDto = RecipeResponseDto(
        id = id,
        authorId = "author",
        author = null,
        title = title,
        description = description,
        note = null,
        imageId = null,
        brewMethod = BrewMethodDto.V60,
        difficulty = DifficultyDto.EASY,
        roastLevel = RoastLevelDto.MEDIUM,
        beans = null,
        steps = null,
        isLiked = isLiked,
        likesCount = likesCount,
        origin = null,
        isPublic = true,
        createdAt = null,
        updatedAt = null,
    )

    fun page(
        ids: List<String>,
        currentPage: Int = 1,
        lastPage: Int = 1,
    ): PageResponseDto<RecipeResponseDto> = PageResponseDto(
        items = ids.map { dto(id = it) },
        pagination = pagination(currentPage = currentPage, lastPage = lastPage, itemsCount = ids.size),
    )

    fun likedPage(
        ids: List<String>,
        currentPage: Int = 1,
        lastPage: Int = 1,
    ): PageResponseDto<LikedRecipeItemDto> = PageResponseDto(
        items = ids.map { LikedRecipeItemDto(likedAt = "2026-05-10T00:00:00Z", recipe = dto(id = it, isLiked = true)) },
        pagination = pagination(currentPage = currentPage, lastPage = lastPage, itemsCount = ids.size),
    )

    private fun pagination(currentPage: Int, lastPage: Int, itemsCount: Int) =
        PaginationResponseDto(
            currentPage = currentPage,
            itemsCount = itemsCount,
            lastPage = lastPage,
            nextPage = if (currentPage < lastPage) currentPage + 1 else currentPage,
        )
}
