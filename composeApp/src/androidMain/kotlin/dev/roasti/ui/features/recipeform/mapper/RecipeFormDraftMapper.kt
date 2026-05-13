package dev.roasti.ui.features.recipeform.mapper

import dev.roasti.feature.recipe.domain.model.RecipeDraft
import dev.roasti.feature.recipe.domain.model.RecipeDraftStep
import dev.roasti.ui.features.recipeform.model.RecipeFormFields
import dev.roasti.ui.features.recipeform.model.RecipeFormStepUiModel

internal fun RecipeFormFields.toRecipeDraft() = RecipeDraft(
    title = title,
    description = description,
    note = null,
    imageId = imageId,
    brewMethod = brewMethod,
    difficulty = difficulty,
    roastLevel = roastLevel,
    beans = beans.takeIf { it.isNotBlank() },
    steps = steps.mapIndexed { index, step -> step.toRecipeDraftStep(index) },
)

private fun RecipeFormStepUiModel.toRecipeDraftStep(index: Int) = RecipeDraftStep(
    order = index,
    title = title,
    description = description.takeIf { it.isNotBlank() },
    durationSeconds = durationSeconds,
    imageId = imageId,
)
