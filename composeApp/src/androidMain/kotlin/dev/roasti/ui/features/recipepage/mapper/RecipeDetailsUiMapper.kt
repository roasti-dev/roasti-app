package dev.roasti.ui.features.recipepage.mapper

import dev.roasti.feature.recipe.domain.model.Recipe
import dev.roasti.feature.recipe.domain.model.BrewStep
import dev.roasti.feature.recipe.domain.model.RoastLevel
import dev.roasti.ui.features.recipe.mapper.iconRes
import dev.roasti.ui.features.recipe.mapper.labelRes
import dev.roasti.ui.features.recipe.mapper.toUiModel
import dev.roasti.ui.features.recipepage.model.RecipeDetailsUiModel
import dev.roasti.ui.features.recipepage.model.RecipeStepUiModel
import dev.roasti.core.utils.imageUrl

internal fun Recipe.toUiModel(currentUserId: String?) = RecipeDetailsUiModel(
    id = id,
    title = title,
    description = description,
    note = note,
    imageUrl = imageId?.let(::imageUrl),
    brewMethodLabelRes = brewMethod.labelRes(),
    brewMethodIconRes = brewMethod.iconRes(),
    difficultyLabelRes = difficulty.labelRes(),
    roastLevelLabelRes = roastLevel.takeUnless { it == RoastLevel.NONE }?.labelRes(),
    beans = beans,
    steps = steps.map(BrewStep::toUiModel),
    isLiked = isLiked,
    likesCount = likesCount,
    author = author?.toUiModel(),
    origin = origin?.toUiModel(),
    isPublic = isPublic,
    totalDurationSeconds = steps.mapNotNull { it.durationSeconds }
        .takeIf { it.isNotEmpty() }
        ?.sum(),
    isOwner = currentUserId != null && author?.id == currentUserId,
)

private fun BrewStep.toUiModel() = RecipeStepUiModel(
    order = order,
    title = title,
    durationSeconds = durationSeconds,
)
