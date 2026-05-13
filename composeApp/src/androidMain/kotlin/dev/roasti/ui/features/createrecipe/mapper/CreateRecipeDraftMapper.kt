package dev.roasti.ui.features.createrecipe.mapper

import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.RecipeDraft
import dev.roasti.feature.recipe.domain.model.RecipeDraftStep
import dev.roasti.feature.recipe.domain.model.RoastLevel
import dev.roasti.ui.features.createrecipe.model.CreateRecipeStepUiModel
import dev.roasti.ui.features.createrecipe.model.CreateRecipeUiState

internal fun CreateRecipeUiState.toRecipeDraft() = RecipeDraft(
    title = name,
    description = description,
    note = null,
    imageId = imageId,
    brewMethod = brewMethod ?: BrewMethod.NONE,
    difficulty = difficulty,
    roastLevel = roastLevel ?: RoastLevel.NONE,
    beans = beans,
    steps = brewSteps.mapIndexed { index, item -> item.toRecipeDraftStep(index) },
)

private fun CreateRecipeStepUiModel.toRecipeDraftStep(index: Int) = RecipeDraftStep(
    order = index,
    title = title,
    description = description.takeIf { it.isNotBlank() },
    durationSeconds = durationInSeconds,
    imageId = imageId,
)
