package dev.roasti.ui.features.favorites.model

import androidx.compose.runtime.Immutable
import dev.roasti.ui.features.recipelist.model.RecipeListItemUiModel

@Immutable
sealed interface FavoritesPreviewState {
    data object Loading : FavoritesPreviewState
    data object Empty : FavoritesPreviewState
    data class Content(
        val items: List<RecipeListItemUiModel>,
        val hasMore: Boolean,
    ) : FavoritesPreviewState
}
