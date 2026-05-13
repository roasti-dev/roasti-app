package dev.roasti.ui.features.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import dev.roasti.feature.recipe.domain.RecipeListsRepository
import dev.roasti.feature.recipe.domain.RecipeRepository
import dev.roasti.ui.features.recipelist.mapper.toUiModel
import dev.roasti.ui.features.recipelist.model.RecipeListItemUiModel
import dev.roasti.ui.uikit.state.UiError
import dev.roasti.ui.uikit.state.UiEvent

class FavoritesViewModel(
    recipeListsRepository: RecipeListsRepository,
    private val recipeRepository: RecipeRepository,
) : ViewModel() {

    private val manualRefreshMutable = MutableStateFlow(false)
    val isManualRefresh: StateFlow<Boolean> = manualRefreshMutable.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    val pagingState: Flow<PagingData<RecipeListItemUiModel>> =
        recipeListsRepository.observeFavorites()
            .map { pagingData -> pagingData.map { it.toUiModel() } }
            .cachedIn(viewModelScope)

    fun likeRecipe(recipe: RecipeListItemUiModel) {
        viewModelScope.launch {
            recipeRepository.toggleLike(recipe.id).onFailure {
                _events.tryEmit(UiEvent.ShowError(UiError.Generic))
            }
        }
    }

    fun startManualRefresh() {
        manualRefreshMutable.value = true
    }

    fun finishManualRefresh() {
        manualRefreshMutable.value = false
    }
}
