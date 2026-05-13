package dev.roasti.ui.features.recipelist

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.bold.Plus
import com.adamglin.phosphoricons.regular.ArrowRight
import org.koin.compose.viewmodel.koinViewModel
import dev.roasti.R
import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.RoastLevel
import dev.roasti.feature.recipe.presentation.filter.RecipeFilterState
import dev.roasti.ui.features.favorites.model.FavoritesPreviewState
import dev.roasti.ui.features.favorites.widgets.FavoritesPreviewRow
import dev.roasti.ui.features.recipelist.components.BrewMethodFilterChip
import dev.roasti.ui.features.recipelist.components.DifficultyFilterChip
import dev.roasti.ui.features.recipelist.components.RecipeCard
import dev.roasti.ui.features.recipelist.components.RecipeSearchBar
import dev.roasti.ui.features.recipelist.components.RoastLevelFilterChip
import dev.roasti.ui.features.recipelist.model.RecipeListItemUiModel
import dev.roasti.ui.theme.RoastiTheme
import dev.roasti.ui.theme.Spacing
import dev.roasti.ui.uikit.ErrorStub
import dev.roasti.ui.uikit.LoadingStub
import dev.roasti.ui.util.recipeImageSharedElementModifier

private const val FavoritesSectionKey = "favorite_recipes_section"
private val RecipeCardHeight = 130.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun RecipesListScreen(
    onRecipeClick: (String) -> Unit = {},
    onCreateClick: () -> Unit = {},
    onSeeAllFavorites: () -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    contentPadding: PaddingValues,
) {
    val viewModel: RecipesListViewModel = koinViewModel()

    val filtersState by viewModel.filtersState.collectAsStateWithLifecycle(RecipeFilterState())
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle("")
    val hasCachedRecipes by viewModel.hasCachedRecipes.collectAsStateWithLifecycle()
    val isDefaultFeedMode by viewModel.isDefaultFeedMode.collectAsStateWithLifecycle()
    val isManualRefresh by viewModel.isManualRefresh.collectAsStateWithLifecycle()
    val recipes = viewModel.pagingRecipesState.collectAsLazyPagingItems()
    val favoritesPreviewState by viewModel.favoritesPreviewState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(isManualRefresh, recipes.loadState.refresh) {
        val recipesDone = recipes.loadState.refresh !is LoadState.Loading
        if (isManualRefresh && recipesDone) {
            viewModel.finishManualRefresh()
        }
    }

    val recipesRefreshState = recipes.loadState.refresh
    val shouldShowFullScreenLoader =
        if (isDefaultFeedMode) {
            !hasCachedRecipes && recipesRefreshState is LoadState.Loading
        } else {
            recipesRefreshState is LoadState.Loading && recipes.itemCount == 0
        }
    val shouldShowFullScreenError =
        if (isDefaultFeedMode) {
            !hasCachedRecipes && recipesRefreshState is LoadState.Error
        } else {
            recipesRefreshState is LoadState.Error && recipes.itemCount == 0
        }
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            shouldShowFullScreenLoader -> {
                LoadingStub(Modifier.align(Alignment.Center))
            }

            shouldShowFullScreenError -> ErrorStub(
                stringResource(R.string.recipes_load_error),
                modifier = Modifier.padding(contentPadding)
            )

            else -> Content(
                searchQuery = searchQuery,
                filtersState = filtersState,
                recipes = recipes,
                favoritesPreviewState = favoritesPreviewState,
                onClick = onRecipeClick,
                onLikeClick = viewModel::likeRecipe,
                onSearch = viewModel::search,
                onRefresh = {
                    viewModel.startManualRefresh()
                    recipes.refresh()
                },
                onSeeAllFavorites = onSeeAllFavorites,
                isManualRefresh = isManualRefresh,
                onBrewMethodSelected = viewModel::filterByBrewMethod,
                onDifficultySelected = viewModel::filterByDifficulty,
                onRoastLevelSelected = viewModel::filterByRoastLevel,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize(),
            )
        }

        FloatingActionButton(
            onClick = onCreateClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = Spacing.lg,
                    bottom = contentPadding.calculateBottomPadding() + Spacing.lg,
                ),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ) {
            Icon(
                imageVector = PhosphorIcons.Bold.Plus,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(22.dp)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = contentPadding.calculateBottomPadding()),
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Content(
    searchQuery: String,
    filtersState: RecipeFilterState,
    recipes: LazyPagingItems<RecipeListItemUiModel>,
    favoritesPreviewState: FavoritesPreviewState,
    onClick: (String) -> Unit,
    onLikeClick: (RecipeListItemUiModel) -> Unit,
    onSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    onSeeAllFavorites: () -> Unit,
    isManualRefresh: Boolean,
    onBrewMethodSelected: (BrewMethod) -> Unit,
    onDifficultySelected: (Difficulty?) -> Unit,
    onRoastLevelSelected: (RoastLevel?) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    PullToRefreshBox(isRefreshing = isManualRefresh, onRefresh = onRefresh, modifier = modifier) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            contentPadding = PaddingValues(
                top = Spacing.sm,
                bottom = contentPadding.calculateBottomPadding() + Spacing.xxxxl,
            ),
        ) {
            stickyHeader(key = "filters") {
                FilterHeader(
                    searchQuery = searchQuery,
                    filtersState = filtersState,
                    onSearch = onSearch,
                    onBrewMethodSelected = onBrewMethodSelected,
                    onDifficultySelected = onDifficultySelected,
                    onRoastLevelSelected = onRoastLevelSelected,
                    modifier = Modifier.animateItem(),
                )
            }

            item(FavoritesSectionKey) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onSeeAllFavorites)
                            .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                    ) {
                        Text(
                            stringResource(R.string.recipe_list_favorite_section_title),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = PhosphorIcons.Regular.ArrowRight,
                            contentDescription = stringResource(R.string.favorites_see_all),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    FavoritesPreviewRow(
                        state = favoritesPreviewState,
                        onItemClick = { onClick(it.id) },
                        onLikeClick = onLikeClick,
                        onSeeAllClick = onSeeAllFavorites,
                    )
                }
            }

            item("all_recipes_title") {
                Text(
                    stringResource(R.string.recipe_list_all_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    modifier = Modifier.padding(start = Spacing.lg)
                )
            }

            items(
                count = recipes.itemCount,
                key = { index -> recipes[index]?.id ?: "recipe_$index" },
            ) { index ->
                val recipe = recipes[index] ?: return@items
                RecipeCard(
                    item = recipe,
                    onLikeClick = { onLikeClick(recipe) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg)
                        .clickable { onClick(recipe.id) }
                        .animateItem(),
                    imageModifier = recipeImageSharedElementModifier(
                        recipeId = recipe.id,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
                )
            }

            if (recipes.itemCount == 0 && recipes.loadState.refresh is LoadState.NotLoading) {
                item("recipes_empty_state") {
                    RecipesEmptyPlaceholderCard(
                        text = stringResource(R.string.recipes_empty_state),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg)
                            .animateItem(),
                    )
                }
            }

            if (recipes.loadState.append is LoadState.Loading) {
                item("recipes_append_loading") {
                    Box(
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .padding(Spacing.lg),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(32.dp)
                                .animateItem(),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipesEmptyPlaceholderCard(
    text: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(RecipeCardHeight),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
        }
    }
}

@Composable
private fun FilterHeader(
    searchQuery: String,
    filtersState: RecipeFilterState,
    onSearch: (String) -> Unit,
    onBrewMethodSelected: (BrewMethod) -> Unit,
    onDifficultySelected: (Difficulty?) -> Unit,
    onRoastLevelSelected: (RoastLevel?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.padding(vertical = Spacing.sm),
        ) {
            RecipeSearchBar(
                query = searchQuery,
                onQueryChange = onSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg),
            ) {
                BrewMethodFilterChip(
                    selectedMethod = filtersState.brewMethod,
                    onMethodSelected = onBrewMethodSelected,
                )
                DifficultyFilterChip(
                    selectedDifficulty = filtersState.difficulty,
                    onDifficultySelected = onDifficultySelected,
                )
                RoastLevelFilterChip(
                    selectedRoastLevel = filtersState.roastLevel,
                    onRoastLevelSelected = onRoastLevelSelected,
                )
            }
        }
    }
}

@Preview
@Composable
private fun RecipesEmptyPlaceholderCardPreview() {
    RoastiTheme {
        RecipesEmptyPlaceholderCard(
            text = "No recipes found",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
