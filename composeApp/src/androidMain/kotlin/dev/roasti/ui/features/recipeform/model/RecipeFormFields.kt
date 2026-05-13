package dev.roasti.ui.features.recipeform.model

import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.RoastLevel

data class RecipeFormFields(
    val title: String = "",
    val description: String = "",
    val imageId: String? = null,
    val imageUrl: String? = null,
    val brewMethod: BrewMethod = BrewMethod.NONE,
    val difficulty: Difficulty = Difficulty.Medium,
    val roastLevel: RoastLevel = RoastLevel.NONE,
    val beans: String = "",
    val steps: List<RecipeFormStepUiModel> = emptyList(),
    val isUploadingImage: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: Boolean = false,
    val activeStepSheet: ActiveStepSheet? = null,
) {
    val canSave: Boolean get() = title.isNotBlank() && !isSaving
}
