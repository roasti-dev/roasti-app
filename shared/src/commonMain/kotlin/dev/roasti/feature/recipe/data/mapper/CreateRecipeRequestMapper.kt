package dev.roasti.feature.recipe.data.mapper

import dev.roasti.feature.recipe.data.remote.model.BrewMethodDto
import dev.roasti.feature.recipe.data.remote.model.DifficultyDto
import dev.roasti.feature.recipe.data.remote.model.RoastLevelDto
import dev.roasti.feature.recipe.data.remote.model.request.CreateRecipeRequestDto
import dev.roasti.feature.recipe.data.remote.model.request.CreateRecipeStepRequestDto
import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.RecipeDraft
import dev.roasti.feature.recipe.domain.model.RecipeDraftStep
import dev.roasti.feature.recipe.domain.model.RoastLevel

fun RecipeDraft.toRequestDto() = CreateRecipeRequestDto(
    title = title,
    beans = beans,
    brewMethod = brewMethod.toRequestDto() ?: BrewMethodDto.NONE,
    description = description,
    note = note,
    difficulty = difficulty.toRequestDto(),
    imageId = imageId,
    roastLevel = roastLevel.toRequestDto(),
    steps = steps.map { it.toRequestDto() },
)

fun RecipeDraftStep.toRequestDto() = CreateRecipeStepRequestDto(
    description = description,
    durationSeconds = durationSeconds,
    imageId = imageId,
    order = order,
    title = title,
)

fun BrewMethod.toRequestDto(): BrewMethodDto? = when (this) {
    BrewMethod.V60 -> BrewMethodDto.V60
    BrewMethod.FrenchPress -> BrewMethodDto.FRENCH_PRESS
    BrewMethod.Aeropress -> BrewMethodDto.AEROPRESS
    BrewMethod.Chemex -> BrewMethodDto.CHEMEX
    BrewMethod.ColdBrew -> BrewMethodDto.COLD_BREW
    BrewMethod.EspressoMachine -> BrewMethodDto.EXPRESSO_MACHINE
    BrewMethod.MokaPot -> BrewMethodDto.MOKA_POT
    BrewMethod.NONE -> null
}

fun Difficulty.toRequestDto(): DifficultyDto = when (this) {
    Difficulty.Easy -> DifficultyDto.EASY
    Difficulty.Medium -> DifficultyDto.MEDIUM
    Difficulty.Hard -> DifficultyDto.HARD
}

fun RoastLevel?.toRequestDto(): RoastLevelDto = when (this) {
    RoastLevel.Light -> RoastLevelDto.LIGHT
    RoastLevel.MediumLight -> RoastLevelDto.MEDIUM_LIGHT
    RoastLevel.Medium -> RoastLevelDto.MEDIUM
    RoastLevel.MediumDark -> RoastLevelDto.MEDIUM_DARK
    RoastLevel.Dark -> RoastLevelDto.DARK
    RoastLevel.NONE, null -> RoastLevelDto.NONE
}

fun Difficulty?.toQueryDto(): DifficultyDto? = when (this) {
    Difficulty.Easy -> DifficultyDto.EASY
    Difficulty.Medium -> DifficultyDto.MEDIUM
    Difficulty.Hard -> DifficultyDto.HARD
    null -> null
}

fun RoastLevel?.toQueryDto(): RoastLevelDto? = when (this) {
    RoastLevel.Light -> RoastLevelDto.LIGHT
    RoastLevel.MediumLight -> RoastLevelDto.MEDIUM_LIGHT
    RoastLevel.Medium -> RoastLevelDto.MEDIUM
    RoastLevel.MediumDark -> RoastLevelDto.MEDIUM_DARK
    RoastLevel.Dark -> RoastLevelDto.DARK
    RoastLevel.NONE, null -> null
}
