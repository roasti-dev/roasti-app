@file:OptIn(ExperimentalLayoutApi::class)

package dev.roasti.ui.features.postcompose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import dev.roasti.R
import dev.roasti.ui.theme.Spacing
import dev.roasti.ui.uikit.LoadingStub
import dev.roasti.ui.uikit.post.PostImageCard
import dev.roasti.ui.uikit.post.PostMediaSource
import dev.roasti.ui.uikit.post.PostMediaSourceSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostComposeScreen(
    postId: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: PostComposeViewModel = koinViewModel(
        key = "post-compose-${postId ?: "new"}",
        parameters = { parametersOf(postId) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                PostComposeEvent.SubmitSuccess -> onClose()
            }
        }
    }

    var showSourceSheet by rememberSaveable { mutableStateOf(false) }

    val galleryPicker = rememberGalleryPicker { fileName, bytes ->
        viewModel.onImagePicked(fileName, bytes)
    }
    val cameraPicker = rememberCameraPicker { fileName, bytes ->
        viewModel.onImagePicked(fileName, bytes)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            PostComposeTopBar(
                mode = state.mode,
                canSubmit = state.canSubmit,
                isSubmitting = state.isSubmitting,
                onClose = onClose,
                onSubmit = viewModel::onSubmit,
            )
        },
        bottomBar = {
            PostComposeBottomBar(
                onAttachClick = { showSourceSheet = true },
            )
        },
    ) { innerPadding ->
        if (state.isLoadingExisting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
            ) {
                LoadingStub(Modifier.align(Alignment.Center))
            }
        } else {
            PostComposeForm(
                state = state,
                onTitleChange = viewModel::onTitleChange,
                onBodyChange = viewModel::onBodyChange,
                onRemoveImage = viewModel::onRemoveImage,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding(),
                    ),
            )
        }
    }

    if (showSourceSheet) {
        PostMediaSourceSheet(
            onDismiss = { showSourceSheet = false },
            onPick = { source ->
                showSourceSheet = false
                when (source) {
                    PostMediaSource.CAMERA -> cameraPicker()
                    PostMediaSource.GALLERY -> galleryPicker()
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostComposeTopBar(
    mode: PostComposeMode,
    canSubmit: Boolean,
    isSubmitting: Boolean,
    onClose: () -> Unit,
    onSubmit: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        title = {
            Text(
                text = stringResource(
                    when (mode) {
                        PostComposeMode.CREATE -> R.string.post_compose_title_create
                        PostComposeMode.EDIT -> R.string.post_compose_title_edit
                    }
                ),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.post_detail_close_label),
                )
            }
        },
        actions = {
            TextButton(
                onClick = onSubmit,
                enabled = canSubmit,
                modifier = Modifier.padding(end = Spacing.sm),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                } else {
                    Text(
                        text = stringResource(
                            when (mode) {
                                PostComposeMode.CREATE -> R.string.post_compose_submit_create
                                PostComposeMode.EDIT -> R.string.post_compose_submit_update
                            }
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        },
    )
}

@Composable
private fun PostComposeBottomBar(
    onAttachClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onAttachClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_image),
                    contentDescription = stringResource(R.string.post_compose_attach_photo),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PostComposeForm(
    state: PostComposeUiState,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onRemoveImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        TitleField(
            value = state.title,
            onValueChange = onTitleChange,
            placeholder = stringResource(R.string.post_compose_title_hint),
        )

        PhotoSection(
            photoState = state.photoState,
            onRemove = onRemoveImage,
        )

        BodyField(
            value = state.body,
            onValueChange = onBodyChange,
            placeholder = stringResource(R.string.post_compose_body_hint),
        )

        if (state.submitError != null) {
            Text(
                text = stringResource(R.string.post_compose_save_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun TitleField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    val titleStyle = MaterialTheme.typography.headlineLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
    )
    Box(modifier = Modifier.fillMaxWidth()) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = titleStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = titleStyle,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.tertiary
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
}

@Composable
private fun BodyField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    val bodyStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
    )
    Box(modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 200.dp)) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = bodyStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = bodyStyle,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.tertiary
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PhotoSection(
    photoState: PhotoState,
    onRemove: () -> Unit,
) {
    when (photoState) {
        PhotoState.None -> Unit
        PhotoState.Uploading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
        is PhotoState.Ready -> {
            Box(modifier = Modifier.fillMaxWidth()) {
                PostImageCard(
                    fullUrl = photoState.previewUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.sm),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_trash),
                        contentDescription = stringResource(R.string.post_compose_remove_photo),
                    )
                }
            }
        }
        PhotoState.Error -> {
            Text(
                text = stringResource(R.string.post_compose_photo_upload_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
