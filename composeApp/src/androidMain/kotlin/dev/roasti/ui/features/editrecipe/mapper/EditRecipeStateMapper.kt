package dev.roasti.ui.features.editrecipe.mapper

import dev.roasti.feature.recipe.domain.model.BrewStep
import dev.roasti.feature.recipe.domain.model.Recipe
import dev.roasti.ui.features.editrecipe.model.EditRecipeUiState
import dev.roasti.ui.features.recipeform.model.RecipeFormFields
import dev.roasti.ui.features.recipeform.model.RecipeFormStepUiModel
import dev.roasti.core.utils.imageUrl

internal fun Recipe.toEditState() = EditRecipeUiState(
    isLoading = false,
    form = RecipeFormFields(
        title = title,
        description = description,
        imageId = imageId,
        imageUrl = imageId?.let(::imageUrl),
        brewMethod = brewMethod,
        difficulty = difficulty,
        roastLevel = roastLevel,
        beans = beans ?: "",
        steps = steps.map(BrewStep::toFormStep),
    ),
)

private fun BrewStep.toFormStep() = RecipeFormStepUiModel(
    order = order,
    title = title,
    description = description,
    durationSeconds = durationSeconds,
    imageId = imageId,
)
