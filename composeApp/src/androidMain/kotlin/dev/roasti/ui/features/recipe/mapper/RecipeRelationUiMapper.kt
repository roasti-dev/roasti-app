package dev.roasti.ui.features.recipe.mapper

import dev.roasti.feature.recipe.domain.model.Author
import dev.roasti.feature.recipe.domain.model.RecipeOrigin
import dev.roasti.ui.features.recipe.model.RecipeAuthorUiModel
import dev.roasti.ui.features.recipe.model.RecipeOriginUiModel

internal fun Author.toUiModel() = RecipeAuthorUiModel(
    id = id,
    username = username,
    avatarId = avatarId,
)

internal fun RecipeOrigin.toUiModel() = RecipeOriginUiModel(
    recipeId = recipeId,
    author = author.toUiModel(),
)
