package dev.roasti.ui.features.recipepage.model

import androidx.annotation.StringRes
import dev.roasti.ui.features.recipe.model.RecipeAuthorUiModel
import dev.roasti.ui.features.recipe.model.RecipeOriginUiModel

data class RecipeDetailsUiModel(
    val id: String,
    val title: String,
    val description: String,
    val note: String? = null,
    val imageUrl: String?,
    @StringRes val brewMethodLabelRes: Int,
    @StringRes val difficultyLabelRes: Int,
    @StringRes val roastLevelLabelRes: Int?,
    val beans: String?,
    val steps: List<RecipeStepUiModel>,
    val isLiked: Boolean,
    val likesCount: Int,
    val author: RecipeAuthorUiModel? = null,
    val origin: RecipeOriginUiModel? = null,
    val isPublic: Boolean? = null,
    val totalDurationSeconds: Int? = null,
    val isOwner: Boolean = false,
)

data class RecipeStepUiModel(
    val order: Int,
    val title: String,
    val durationSeconds: Int?,
)
