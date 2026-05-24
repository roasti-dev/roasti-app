package dev.roasti.feature.recipe.data.mapper

import dev.roasti.feature.recipe.data.remote.model.BrewMethodDto
import dev.roasti.feature.recipe.data.remote.model.DifficultyDto
import dev.roasti.feature.recipe.data.remote.model.RoastLevelDto
import dev.roasti.feature.recipe.data.remote.model.response.AuthorResponseDto
import dev.roasti.feature.recipe.data.remote.model.response.RecipeOriginResponseDto
import dev.roasti.feature.recipe.data.remote.model.response.RecipeResponseDto
import dev.roasti.feature.recipe.data.remote.model.response.RecipeStepResponseDto
import dev.roasti.core.network.PageResponseDto
import dev.roasti.feature.recipe.domain.model.Author
import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.BrewStep
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.Recipe
import dev.roasti.feature.recipe.domain.model.RecipeOrigin
import dev.roasti.core.domain.Page
import dev.roasti.feature.recipe.domain.model.RoastLevel

fun PageResponseDto<RecipeResponseDto>.toDomain() = Page(
    items = items.map { it.toDomain() },
    currentPage = pagination.currentPage,
    itemsCount = pagination.itemsCount,
    lastPage = pagination.lastPage,
    nextPage = pagination.nextPage,
)

fun RecipeResponseDto.toDomain(): Recipe = Recipe(
    id = id,
    title = title,
    description = description,
    note = note,
    imageId = imageId,
    brewMethod = brewMethod.toDomain(),
    difficulty = difficulty.toDomain(),
    roastLevel = roastLevel.toDomain(),
    beans = beans,
    steps = steps.orEmpty().map(RecipeStepResponseDto::toDomain),
    author = author?.toDomain(),
    isLiked = isLiked,
    likesCount = likesCount,
    origin = origin?.toDomain(),
    isPublic = isPublic,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun AuthorResponseDto.toDomain(): Author = Author(
    id = id,
    username = username,
    avatarId = avatarId,
)

fun RecipeOriginResponseDto.toDomain(): RecipeOrigin = RecipeOrigin(
    author = author.toDomain(),
    recipeId = recipeId,
)

fun RecipeStepResponseDto.toDomain(): BrewStep = BrewStep(
    order = order,
    title = title,
    durationSeconds = durationSeconds,
    imageId = imageId,
)

fun BrewMethodDto?.toDomain(): BrewMethod = when (this) {
    BrewMethodDto.V60 -> BrewMethod.V60
    BrewMethodDto.FRENCH_PRESS -> BrewMethod.FrenchPress
    BrewMethodDto.AEROPRESS -> BrewMethod.Aeropress
    BrewMethodDto.CHEMEX -> BrewMethod.Chemex
    BrewMethodDto.COLD_BREW -> BrewMethod.ColdBrew
    BrewMethodDto.EXPRESSO_MACHINE -> BrewMethod.EspressoMachine
    BrewMethodDto.MOKA_POT -> BrewMethod.MokaPot
    BrewMethodDto.NONE, null -> BrewMethod.NONE
}

fun DifficultyDto.toDomain(): Difficulty = when (this) {
    DifficultyDto.EASY -> Difficulty.Easy
    DifficultyDto.MEDIUM -> Difficulty.Medium
    DifficultyDto.HARD -> Difficulty.Hard
}

fun RoastLevelDto?.toDomain(): RoastLevel = when (this) {
    RoastLevelDto.LIGHT -> RoastLevel.Light
    RoastLevelDto.MEDIUM_LIGHT -> RoastLevel.MediumLight
    RoastLevelDto.MEDIUM -> RoastLevel.Medium
    RoastLevelDto.MEDIUM_DARK -> RoastLevel.MediumDark
    RoastLevelDto.DARK -> RoastLevel.Dark
    RoastLevelDto.NONE, null -> RoastLevel.NONE
}
