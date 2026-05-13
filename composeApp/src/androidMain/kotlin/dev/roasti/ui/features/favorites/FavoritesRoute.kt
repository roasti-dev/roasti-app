package dev.roasti.ui.features.favorites

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import org.koin.compose.viewmodel.koinViewModel
import dev.roasti.R
import dev.roasti.ui.features.recipelist.components.RecipeCard
import dev.roasti.ui.theme.Spacing
import dev.roasti.ui.uikit.ErrorStub
import dev.roasti.ui.uikit.LoadingStub
import dev.roasti.ui.util.recipeImageSharedElementModifier
import dev.roasti.ui.uikit.state.EventEffect

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FavoritesRoute(
    onBackClick: () -> Unit,
    onRecipeClick: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val viewModel: FavoritesViewModel = koinViewModel()
    val favorites = viewModel.pagingState.collectAsLazyPagingItems()
    val isManualRefresh by viewModel.isManualRefresh.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    EventEffect(viewModel.events, snackbarHostState)

    LaunchedEffect(isManualRefresh, favorites.loadState.refresh) {
        if (isManualRefresh && favorites.loadState.refresh !is LoadState.Loading) {
            viewModel.finishManualRefresh()
        }
    }

    val refreshState = favorites.loadState.refresh
    val showFullScreenLoader = refreshState is LoadState.Loading && favorites.itemCount == 0
    val showFullScreenError = refreshState is LoadState.Error && favorites.itemCount == 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recipe_list_favorite_section_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.ArrowLeft,
                            contentDescription = stringResource(R.string.back_label),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                showFullScreenLoader -> LoadingStub(Modifier.align(Alignment.Center))

                showFullScreenError -> ErrorStub(
                    text = stringResource(R.string.favorites_load_error),
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> PullToRefreshBox(
                    isRefreshing = isManualRefresh,
                    onRefresh = {
                        viewModel.startManualRefresh()
                        favorites.refresh()
                    },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        state = rememberLazyListState(),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        contentPadding = PaddingValues(
                            top = Spacing.sm,
                            bottom = Spacing.xxxxl,
                        ),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            count = favorites.itemCount,
                            key = { index -> favorites[index]?.id ?: "favorite_$index" },
                        ) { index ->
                            val recipe = favorites[index] ?: return@items
                            RecipeCard(
                                item = recipe,
                                onLikeClick = { viewModel.likeRecipe(recipe) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.lg)
                                    .clickable { onRecipeClick(recipe.id) }
                                    .animateItem(),
                                imageModifier = recipeImageSharedElementModifier(
                                    recipeId = recipe.id,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                ),
                            )
                        }

                        if (favorites.loadState.append is LoadState.Loading) {
                            item("favorites_append_loading") {
                                Box(
                                    modifier = Modifier
                                        .fillParentMaxWidth()
                                        .padding(Spacing.lg),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        color = MaterialTheme.colorScheme.secondary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
