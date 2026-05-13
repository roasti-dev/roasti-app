package dev.roasti.ui.features.feed

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import org.koin.compose.viewmodel.koinViewModel
import dev.roasti.ui.components.bottomBarAware
import dev.roasti.ui.features.feed.model.PostUiModel
import dev.roasti.ui.theme.Spacing
import dev.roasti.ui.uikit.ErrorStub
import dev.roasti.ui.uikit.LoadingStub
import dev.roasti.ui.uikit.SearchInput
import dev.roasti.R
import dev.roasti.ui.components.LocalBottomBarScrollBehavior
import dev.roasti.ui.uikit.post.DeletePostConfirmDialog
import dev.roasti.ui.uikit.post.PostCard
import dev.roasti.ui.uikit.post.PostOwnerActionsSheet
import dev.roasti.ui.uikit.post.PostUserReaction
import dev.roasti.ui.util.postCardSharedBoundsModifier

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun FeedScreen(
    contentPadding: PaddingValues,
    onPostClick: (String) -> Unit,
    onCreatePost: () -> Unit,
    onEditPost: (String) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val viewModel: FeedViewModel = koinViewModel()

    val isManualRefresh by viewModel.isManualRefresh.collectAsStateWithLifecycle()
    val hasCachedPosts by viewModel.hasCachedPosts.collectAsStateWithLifecycle()
    val posts = viewModel.pagingPostsState.collectAsLazyPagingItems()
    val listState = rememberLazyListState()

    LaunchedEffect(isManualRefresh, posts.loadState.refresh) {
        if (isManualRefresh && posts.loadState.refresh !is LoadState.Loading) {
            viewModel.finishManualRefresh()
        }
    }

    val refreshState = posts.loadState.refresh
    val appendState = posts.loadState.append

    // PagingData is cached in viewModelScope, so a stale Error from a previous attempt
    // is visible immediately on re-entry. Auto-retry once on entry, and suppress the
    // fullscreen error until we've actually observed a Loading transition.
    var hasObservedLoading by remember { mutableStateOf(false) }
    LaunchedEffect(refreshState) {
        if (refreshState is LoadState.Loading) hasObservedLoading = true
    }
    LaunchedEffect(Unit) {
        if (posts.loadState.refresh is LoadState.Error) posts.retry()
    }

    val isInitialIdle = refreshState is LoadState.NotLoading && !refreshState.endOfPaginationReached
    val showFullScreenLoader = !hasCachedPosts && (
        refreshState is LoadState.Loading ||
            isInitialIdle ||
            (refreshState is LoadState.Error && !hasObservedLoading)
        )
    val showFullScreenError = !hasCachedPosts &&
        refreshState is LoadState.Error &&
        hasObservedLoading

    val snackbarHostState = remember { SnackbarHostState() }
    val refreshErrorMessage = stringResource(R.string.feed_refresh_error)
    LaunchedEffect(refreshState, hasCachedPosts) {
        if (hasCachedPosts && refreshState is LoadState.Error) {
            snackbarHostState.showSnackbar(refreshErrorMessage)
        }
    }
    LaunchedEffect(appendState) {
        if (appendState is LoadState.Error) {
            snackbarHostState.showSnackbar(refreshErrorMessage)
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var ownerActionsFor by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDeletePostId by rememberSaveable { mutableStateOf<String?>(null) }

    val bottomBarBehavior = LocalBottomBarScrollBehavior.current

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0),
        topBar = { FeedTopBar(scrollBehavior = scrollBehavior) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreatePost,
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = contentPadding.calculateBottomPadding())
                    .graphicsLayer {
                        translationY = -(bottomBarBehavior?.heightOffsetPx ?: 0f)
                    },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_chat),
                    contentDescription = stringResource(R.string.post_create_fab_label),
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
    ) { innerPadding ->
        // LazyColumn fills the full body and pads its FIRST item below the TopAppBar via
        // contentPadding.top — so as the user scrolls, items pass under the bar and (once
        // the bar collapses via enterAlways) under the transparent system status bar.
        when {
            showFullScreenLoader -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
            ) {
                LoadingStub(Modifier.align(Alignment.Center))
            }

            showFullScreenError -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
            ) {
                ErrorStub(
                    text = stringResource(R.string.feed_load_error),
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            else -> FeedContent(
                posts = posts,
                listState = listState,
                isManualRefresh = isManualRefresh,
                onRefresh = {
                    viewModel.startManualRefresh()
                    posts.refresh()
                },
                onRatingChange = viewModel::onRatingChange,
                onPostClick = onPostClick,
                onImageClick = onImageClick,
                onOwnerOptionsClick = { post -> ownerActionsFor = post.id },
                topInset = innerPadding.calculateTopPadding(),
                bottomInset = contentPadding.calculateBottomPadding(),
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    }

    val activeOwnerPostId = ownerActionsFor
    if (activeOwnerPostId != null) {
        PostOwnerActionsSheet(
            onEdit = {
                ownerActionsFor = null
                onEditPost(activeOwnerPostId)
            },
            onDelete = {
                ownerActionsFor = null
                pendingDeletePostId = activeOwnerPostId
            },
            onDismiss = { ownerActionsFor = null },
        )
    }

    val deletingId = pendingDeletePostId
    if (deletingId != null) {
        DeletePostConfirmDialog(
            onConfirm = {
                viewModel.onDeletePost(deletingId)
                pendingDeletePostId = null
            },
            onDismiss = { pendingDeletePostId = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    val searchState = rememberTextFieldState()
    TopAppBar(
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        title = {
            // Left padding is supplied by TopAppBar's title slot (16dp by spec).
            // We add matching end padding to balance the right edge.
            SearchInput(
                state = searchState,
                placeholder = "Search blends, roasts...",
                enabled = false,
                onClick = {
                    // TODO: navigate to SearchScreen when search backend lands
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = Spacing.lg),
            )
        },
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun FeedContent(
    posts: LazyPagingItems<PostUiModel>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isManualRefresh: Boolean,
    onRefresh: () -> Unit,
    onRatingChange: (PostUiModel, PostUserReaction) -> Unit,
    onPostClick: (String) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    onOwnerOptionsClick: (PostUiModel) -> Unit,
    topInset: androidx.compose.ui.unit.Dp,
    bottomInset: androidx.compose.ui.unit.Dp,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(
        isRefreshing = isManualRefresh,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            contentPadding = PaddingValues(
                top = topInset,
                bottom = bottomInset + Spacing.xxxl,
            ),
            modifier = Modifier
                .fillMaxSize()
                .bottomBarAware(listState),
        ) {
            items(
                count = posts.itemCount,
                key = { index -> posts[index]?.id ?: "post_$index" },
            ) { index ->
                val post = posts[index] ?: return@items
                PostCard(
                    authorImageUrl = post.authorImageUrl,
                    authorName = post.authorName,
                    postedAt = post.postedAt,
                    title = post.title,
                    body = post.body,
                    postImageUrl = post.postImageUrl,
                    ratingState = post.ratingState,
                    commentsCount = post.commentsCount,
                    isOwn = post.isOwn,
                    onRatingChange = { intent -> onRatingChange(post, intent) },
                    onClick = { onPostClick(post.id) },
                    onCommentsClick = { onPostClick(post.id) },
                    onOwnerOptionsClick = { onOwnerOptionsClick(post) },
                    onImageClick = {
                        post.postImageUrl?.let { url -> onImageClick(listOf(url), 0) }
                    },
                    imageModifier = post.postImageUrl?.let { url ->
                        dev.roasti.ui.uikit.photoviewer.photoSharedBoundsModifier(
                            imageUrl = url,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                    } ?: Modifier,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            postCardSharedBoundsModifier(
                                postId = post.id,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                            )
                        ),
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }

            val refresh = posts.loadState.refresh
            val isTrulyEmpty = posts.itemCount == 0 &&
                refresh is LoadState.NotLoading && refresh.endOfPaginationReached
            if (isTrulyEmpty) {
                item("feed_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.xxxl),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No posts yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (posts.loadState.append is LoadState.Loading) {
                item("feed_append_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
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
