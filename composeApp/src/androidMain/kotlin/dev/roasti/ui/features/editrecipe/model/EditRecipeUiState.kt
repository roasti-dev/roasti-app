package dev.roasti.ui.features.editrecipe.model

import dev.roasti.ui.features.recipeform.model.RecipeFormFields

data class EditRecipeUiState(
    val isLoading: Boolean = true,
    val loadError: Boolean = false,
    val form: RecipeFormFields = RecipeFormFields(),
) {
    val canSave: Boolean get() = form.canSave && !isLoading
    val isEditing: Boolean get() = !isLoading && !loadError
}
