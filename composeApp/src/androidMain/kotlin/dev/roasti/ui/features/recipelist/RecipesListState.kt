package dev.roasti.ui.features.recipelist

import dev.roasti.ui.features.recipelist.model.RecipeListItemUiModel

sealed interface RecipesListState {
    data object Loading : RecipesListState
    data object Error : RecipesListState
    data class Content(
        val recipes: List<RecipeListItemUiModel>,
        val isRefreshing: Boolean = false, // for pull to refresh, used when we reload our data
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = true,
        val currentPage: Int = 1,
        val nextPage: Int? = null,
    ) : RecipesListState
}


sealed interface FavoritesRecipesState {
    object Empty : FavoritesRecipesState
    data class Content(
        val items: List<RecipeListItemUiModel>,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = true,
        val currentPage: Int = 1,
        val nextPage: Int? = null,
    ) : FavoritesRecipesState
}