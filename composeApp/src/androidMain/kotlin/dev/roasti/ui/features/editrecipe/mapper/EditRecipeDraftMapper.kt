package dev.roasti.ui.features.editrecipe.mapper

import dev.roasti.feature.recipe.domain.model.RecipeDraft
import dev.roasti.ui.features.editrecipe.model.EditRecipeUiState
import dev.roasti.ui.features.recipeform.mapper.toRecipeDraft

internal fun EditRecipeUiState.toRecipeDraft(): RecipeDraft = form.toRecipeDraft()
