package dev.roasti.ui.features.recipelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dev.roasti.feature.auth.domain.repository.AuthRepository
import dev.roasti.feature.likes.data.LikesApiClient
import dev.roasti.feature.likes.data.toDomain
import dev.roasti.feature.recipe.domain.RecipeListsRepository
import dev.roasti.feature.recipe.domain.RecipeRepository
import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.RecipesPagingQuery
import dev.roasti.feature.recipe.domain.model.RoastLevel
import dev.roasti.feature.recipe.presentation.filter.RecipeFilterState
import dev.roasti.feature.recipe.presentation.filter.RecipeFilterStore
import dev.roasti.ui.features.favorites.model.FavoritesPreviewState
import dev.roasti.ui.features.recipelist.mapper.toUiModel
import dev.roasti.ui.features.recipelist.model.RecipeListItemUiModel
import dev.roasti.utils.stateInWhileSubscribe

private const val SearchQueryDebounceMillis = 300L
private const val FavoritesPreviewLimit = 20
private const val FavoritesPreviewVisibleLimit = FavoritesPreviewLimit - 1

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class RecipesListViewModel(
    private val filterStore: RecipeFilterStore,
    private val recipeListsRepository: RecipeListsRepository,
    private val recipeRepository: RecipeRepository,
    private val authRepository: AuthRepository,
    private val likesApiClient: LikesApiClient,
) : ViewModel() {
    val hasCachedRecipes: StateFlow<Boolean> =
        recipeListsRepository.observeHasCachedFeed()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = false,
            )

    val filtersState: Flow<RecipeFilterState> = filterStore.state

    private val searchQueryMutable = MutableStateFlow("")
    val searchQuery: Flow<String> = searchQueryMutable.asStateFlow()

    private val manualRefreshMutable = MutableStateFlow(false)
    val isManualRefresh: StateFlow<Boolean> = manualRefreshMutable.asStateFlow()

    private val recipesQuery: Flow<RecipesPagingQuery> =
        combine(
            searchQueryMutable
                .debounce { query ->
                    if (query.isBlank()) {
                        0L
                    } else {
                        SearchQueryDebounceMillis
                    }
                }
                .map(String::trim)
                .distinctUntilChanged(),
            filterStore.state,
        ) { query, filters ->
            RecipesPagingQuery(
                query = query,
                brewMethod = filters.brewMethod,
                difficulty = filters.difficulty,
                roastLevel = filters.roastLevel,
            )
        }.distinctUntilChanged()

    val isDefaultFeedMode: StateFlow<Boolean> =
        recipesQuery
            .map { query -> query.isDefaultFeed }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = true,
            )

    val pagingRecipesState: Flow<PagingData<RecipeListItemUiModel>> =
        recipesQuery
            .flatMapLatest { query ->
                if (query.isDefaultFeed) {
                    recipeListsRepository.observeFeed()
                } else {
                    recipeListsRepository.observeSearch(query)
                }
            }
            .map { pagingData -> pagingData.map { it.toUiModel() } }
            .cachedIn(viewModelScope)

    private val favoritesRefreshTrigger = MutableStateFlow(0)

    val favoritesPreviewState: StateFlow<FavoritesPreviewState> = combine(
        authRepository.getUser(),
        favoritesRefreshTrigger,
    ) { user, _ -> user?.id }
        .flatMapLatest { userId -> favoritesPreviewFlow(userId) }
        .stateInWhileSubscribe(FavoritesPreviewState.Loading)

    private fun favoritesPreviewFlow(userId: String?) = flow {
        if (userId == null) {
            emit(FavoritesPreviewState.Empty)
            return@flow
        }
        val result = likesApiClient.getLikedRecipes(
            userId = userId,
            limit = FavoritesPreviewLimit,
            page = 1,
        ).map { it.toDomain() }
        val likes = result.getOrNull()
        if (likes?.items.isNullOrEmpty()) {
            emit(FavoritesPreviewState.Empty)
        } else {
            emit(
                FavoritesPreviewState.Content(
                    items = likes.items.map { it.recipe.toUiModel() }.take(FavoritesPreviewVisibleLimit),
                    hasMore = likes.items.size > FavoritesPreviewVisibleLimit,
                )
            )
        }
    }

    fun search(query: String) {
        searchQueryMutable.value = query
    }

    fun filterByBrewMethod(method: BrewMethod) {
        filterStore.applyFilter(method.takeIf { it != BrewMethod.NONE })
    }

    fun filterByDifficulty(difficulty: Difficulty?) {
        filterStore.applyFilter(difficulty)
    }

    fun filterByRoastLevel(roastLevel: RoastLevel?) {
        filterStore.applyFilter(roastLevel)
    }

    fun likeRecipe(recipe: RecipeListItemUiModel) {
        viewModelScope.launch {
            recipeRepository.toggleLike(recipe.id)
        }
    }

    fun startManualRefresh() {
        manualRefreshMutable.value = true
        favoritesRefreshTrigger.update { it + 1 }
    }

    fun finishManualRefresh() {
        manualRefreshMutable.value = false
    }
}
