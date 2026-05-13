package dev.roasti.ui.features.postdetail

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import dev.roasti.R
import dev.roasti.ui.features.feed.model.PostUiModel
import dev.roasti.ui.features.postdetail.model.CommentThreadUiModel
import dev.roasti.ui.features.postdetail.model.CommentUiModel
import dev.roasti.ui.theme.Spacing
import dev.roasti.ui.uikit.ErrorStub
import dev.roasti.ui.uikit.LoadingStub
import dev.roasti.ui.uikit.comment.CommentComposer
import dev.roasti.ui.uikit.comment.CommentComposerListener
import dev.roasti.ui.uikit.comment.CommentItem
import dev.roasti.ui.uikit.comment.CommentOwnerActionsSheet
import dev.roasti.ui.uikit.comment.CommentsEmptyState
import dev.roasti.ui.uikit.comment.DeleteCommentConfirmDialog
import dev.roasti.ui.uikit.post.DeletePostConfirmDialog
import dev.roasti.ui.uikit.post.PostCard
import dev.roasti.ui.uikit.post.PostOwnerActionsSheet
import dev.roasti.ui.util.postCardSharedBoundsModifier

interface CommentInteractionListener {
    fun onMoreClick(comment: CommentUiModel)
    fun onReplyClick(comment: CommentUiModel)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PostDetailScreen(
    postId: String,
    onClose: () -> Unit,
    onEditPost: (String) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val viewModel: PostDetailViewModel = koinViewModel(parameters = { parametersOf(postId) })
    val headerState by viewModel.headerState.collectAsStateWithLifecycle()
    val composerState by viewModel.composer.collectAsStateWithLifecycle()
    val comments = viewModel.commentsPager.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val isOwn = (headerState as? PostDetailViewModel.HeaderState.Content)?.post?.isOwn == true
    var showOwnerSheet by rememberSaveable { mutableStateOf(false) }
    var showDeletePostDialog by rememberSaveable { mutableStateOf(false) }
    var commentSheetForId by rememberSaveable { mutableStateOf<String?>(null) }
    var commentToDeleteId by rememberSaveable { mutableStateOf<String?>(null) }

    val commentSheetTarget = remember(commentSheetForId, comments.itemCount) {
        commentSheetForId?.let { id -> findCommentById(comments, id) }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                PostDetailViewModel.PostDetailEvent.DeleteSuccess -> onClose()
                PostDetailViewModel.PostDetailEvent.CreateError ->
                    Toast.makeText(context, R.string.comments_create_error, Toast.LENGTH_SHORT).show()
                PostDetailViewModel.PostDetailEvent.EditError ->
                    Toast.makeText(context, R.string.comments_edit_error, Toast.LENGTH_SHORT).show()
                PostDetailViewModel.PostDetailEvent.DeleteCommentError ->
                    Toast.makeText(context, R.string.comments_delete_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val commentListener = remember(viewModel) {
        object : CommentInteractionListener {
            override fun onMoreClick(comment: CommentUiModel) { commentSheetForId = comment.id }
            override fun onReplyClick(comment: CommentUiModel) { viewModel.onStartReply(comment) }
        }
    }

    val composerListener = remember(viewModel) {
        object : CommentComposerListener {
            override fun onTextChange(text: String) = viewModel.onComposerTextChange(text)
            override fun onSubmit() = viewModel.onComposerSubmit()
            override fun onCancelMode() = viewModel.onCancelComposerMode()
        }
    }

    val replyingToAuthor = (composerState.mode as? PostDetailViewModel.ComposerMode.Reply)?.parentAuthor
    val isEditing = composerState.mode is PostDetailViewModel.ComposerMode.Edit

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            PostDetailTopBar(
                onClose = onClose,
                showOwnerOptions = isOwn,
                onOwnerOptionsClick = { showOwnerSheet = true },
            )
        },
        bottomBar = {
            CommentComposer(
                text = composerState.text,
                isEditing = isEditing,
                replyingToAuthor = replyingToAuthor,
                isSubmitting = composerState.isSubmitting,
                listener = composerListener,
            )
        },
    ) { innerPadding ->
        when (val state = headerState) {
            PostDetailViewModel.HeaderState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
            ) {
                LoadingStub(Modifier.align(Alignment.Center))
            }

            PostDetailViewModel.HeaderState.Error -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
            ) {
                ErrorStub(
                    text = stringResource(R.string.post_detail_load_error),
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            is PostDetailViewModel.HeaderState.Content -> PostDetailContent(
                post = state.post,
                comments = comments,
                listState = listState,
                topInset = innerPadding.calculateTopPadding(),
                bottomInset = innerPadding.calculateBottomPadding(),
                onRatingChange = viewModel::onRatingChange,
                onImageClick = onImageClick,
                commentListener = commentListener,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    }

    if (showOwnerSheet) {
        PostOwnerActionsSheet(
            onEdit = {
                showOwnerSheet = false
                onEditPost(postId)
            },
            onDelete = {
                showOwnerSheet = false
                showDeletePostDialog = true
            },
            onDismiss = { showOwnerSheet = false },
        )
    }

    if (showDeletePostDialog) {
        DeletePostConfirmDialog(
            onConfirm = {
                showDeletePostDialog = false
                viewModel.onDeletePost()
            },
            onDismiss = { showDeletePostDialog = false },
        )
    }

    commentSheetTarget?.let { target ->
        CommentOwnerActionsSheet(
            onEdit = {
                commentSheetForId = null
                viewModel.onStartEdit(target)
            },
            onDelete = {
                commentSheetForId = null
                commentToDeleteId = target.id
            },
            onDismiss = { commentSheetForId = null },
        )
    }

    commentToDeleteId?.let { id ->
        DeleteCommentConfirmDialog(
            onConfirm = {
                commentToDeleteId = null
                viewModel.onDeleteComment(id)
            },
            onDismiss = { commentToDeleteId = null },
        )
    }
}

private fun findCommentById(
    items: LazyPagingItems<CommentThreadUiModel>,
    id: String,
): CommentUiModel? {
    for (i in 0 until items.itemCount) {
        val thread = items.peek(i) ?: continue
        if (thread.root.id == id) return thread.root
        thread.replies.firstOrNull { it.id == id }?.let { return it }
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostDetailTopBar(
    onClose: () -> Unit,
    showOwnerOptions: Boolean,
    onOwnerOptionsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        title = {},
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.post_detail_close_label),
                )
            }
        },
        actions = {
            if (showOwnerOptions) {
                IconButton(onClick = onOwnerOptionsClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_three_dots),
                        contentDescription = stringResource(R.string.post_detail_more_options),
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PostDetailContent(
    post: PostUiModel,
    comments: LazyPagingItems<CommentThreadUiModel>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    topInset: androidx.compose.ui.unit.Dp,
    bottomInset: androidx.compose.ui.unit.Dp,
    onRatingChange: (dev.roasti.ui.uikit.post.PostUserReaction) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    commentListener: CommentInteractionListener,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    modifier: Modifier = Modifier,
) {
    val refreshState = comments.loadState.refresh
    val appendState = comments.loadState.append

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(top = topInset, bottom = bottomInset + Spacing.xxxl),
        modifier = modifier.fillMaxSize(),
    ) {
        item("post_header") {
            PostCard(
                authorImageUrl = post.authorImageUrl,
                authorName = post.authorName,
                postedAt = post.postedAt,
                title = post.title,
                body = post.body,
                postImageUrl = post.postImageUrl,
                ratingState = post.ratingState,
                commentsCount = post.commentsCount,
                isExpanded = true,
                onRatingChange = onRatingChange,
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
        }

        item("post_divider") {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                thickness = 8.dp,
            )
        }

        if (comments.itemCount > 0) {
            item("comments_section_title") {
                Text(
                    text = stringResource(R.string.comments_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        horizontal = Spacing.lg,
                        vertical = Spacing.md,
                    ),
                )
            }
        }

        items(
            count = comments.itemCount,
            key = { index -> comments[index]?.root?.id ?: "thread_$index" },
        ) { index ->
            val thread = comments[index] ?: return@items
            CommentThreadBlock(
                thread = thread,
                listener = commentListener,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
        }

        if (comments.itemCount == 0 && refreshState is LoadState.NotLoading) {
            item("comments_empty") {
                CommentsEmptyState(
                    title = stringResource(R.string.comments_empty_title),
                    subtitle = stringResource(R.string.comments_empty_subtitle),
                )
            }
        }

        if (comments.itemCount == 0 && refreshState is LoadState.Loading) {
            item("comments_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.xxl),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }

        if (refreshState is LoadState.Error) {
            item("comments_error") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.xxl),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.comments_load_error),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (appendState is LoadState.Loading) {
            item("comments_append_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentThreadBlock(
    thread: CommentThreadUiModel,
    listener: CommentInteractionListener,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(vertical = Spacing.xs)) {
        CommentItem(
            isDeleted = thread.root.isDeleted,
            authorName = thread.root.authorName,
            authorAvatarUrl = thread.root.authorAvatarUrl,
            postedAt = thread.root.postedAt,
            body = thread.root.body,
            isOwn = thread.root.isOwn,
            showReply = true,
            onMoreClick = { listener.onMoreClick(thread.root) },
            onReplyClick = { listener.onReplyClick(thread.root) },
        )
        thread.replies.forEach { reply ->
            ReplyRow(reply = reply, listener = listener)
        }
    }
}

@Composable
private fun ReplyRow(
    reply: CommentUiModel,
    listener: CommentInteractionListener,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = Spacing.lg, top = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        CommentItem(
            isDeleted = reply.isDeleted,
            authorName = reply.authorName,
            authorAvatarUrl = reply.authorAvatarUrl,
            postedAt = reply.postedAt,
            body = reply.body,
            isOwn = reply.isOwn,
            showReply = false,
            onMoreClick = { listener.onMoreClick(reply) },
        )
    }
}
