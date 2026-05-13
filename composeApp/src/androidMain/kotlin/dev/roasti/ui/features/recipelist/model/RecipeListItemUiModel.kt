package dev.roasti.ui.features.recipelist.model

import androidx.annotation.StringRes
import dev.roasti.ui.features.recipe.model.RecipeAuthorUiModel
import dev.roasti.ui.features.recipe.model.RecipeOriginUiModel

data class RecipeListItemUiModel(
    val id: String,
    val title: String,
    val description: String,
    val note: String? = null,
    val imageUrl: String?,
    @StringRes val brewMethodLabelRes: Int,
    @StringRes val difficultyLabelRes: Int,
    val isLiked: Boolean,
    val likesCount: Int,
    val author: RecipeAuthorUiModel? = null,
    val origin: RecipeOriginUiModel? = null,
    val isPublic: Boolean? = null,
)
