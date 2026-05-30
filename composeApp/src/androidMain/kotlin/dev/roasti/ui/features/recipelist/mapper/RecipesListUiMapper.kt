package dev.roasti.ui.features.recipelist.mapper

import dev.roasti.core.utils.imageUrl
import dev.roasti.feature.recipe.domain.model.Recipe
import dev.roasti.ui.features.recipe.mapper.iconRes
import dev.roasti.ui.features.recipe.mapper.labelRes
import dev.roasti.ui.features.recipe.mapper.toUiModel
import dev.roasti.ui.features.recipelist.model.RecipeListItemUiModel

internal fun Recipe.toUiModel() = RecipeListItemUiModel(
    id = id,
    title = title,
    description = description,
    note = note,
    imageUrl = imageId?.let(::imageUrl),
    brewMethodLabelRes = brewMethod.labelRes(),
    brewMethodIconRes = brewMethod.iconRes(),
    difficultyLabelRes = difficulty.labelRes(),
    isLiked = isLiked,
    likesCount = likesCount,
    author = author?.toUiModel(),
    origin = origin?.toUiModel(),
    isPublic = isPublic,
)
