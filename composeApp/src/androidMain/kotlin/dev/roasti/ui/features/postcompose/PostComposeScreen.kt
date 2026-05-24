@file:OptIn(ExperimentalLayoutApi::class)

package dev.roasti.ui.features.postcompose

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import dev.roasti.R
import dev.roasti.ui.theme.Spacing
import dev.roasti.ui.uikit.LoadingStub
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
    val context = LocalContext.current
    val maxReachedMessage = stringResource(R.string.post_compose_photos_max_reached, MAX_POST_PHOTOS)

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                PostComposeEvent.SubmitSuccess -> onClose()
                PostComposeEvent.MaxPhotosReached ->
                    Toast.makeText(context, maxReachedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    var showSourceSheet by rememberSaveable { mutableStateOf(false) }

    val galleryPicker = rememberGalleryPicker(viewModel::onImagesPicked)
    val cameraPicker = rememberCameraPicker(viewModel::onImagesPicked)

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
                isAttachEnabled = state.canAddMorePhotos,
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
                onRemovePhoto = viewModel::onRemovePhoto,
                onAddPhotos = { showSourceSheet = true },
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
    isAttachEnabled: Boolean,
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
            IconButton(onClick = onAttachClick, enabled = isAttachEnabled) {
                Icon(
                    painter = painterResource(R.drawable.ic_image),
                    contentDescription = stringResource(R.string.post_compose_attach_photo),
                    tint = if (isAttachEnabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    },
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
    onRemovePhoto: (localId: String) -> Unit,
    onAddPhotos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        TitleField(
            value = state.title,
            onValueChange = onTitleChange,
            placeholder = stringResource(R.string.post_compose_title_hint),
            modifier = Modifier.padding(horizontal = Spacing.lg),
        )

        if (state.photos.isNotEmpty() || state.canAddMorePhotos) {
            PhotosStrip(
                photos = state.photos,
                canAddMore = state.canAddMorePhotos,
                onRemove = onRemovePhoto,
                onAdd = onAddPhotos,
            )
        }

        BodyField(
            value = state.body,
            onValueChange = onBodyChange,
            placeholder = stringResource(R.string.post_compose_body_hint),
            modifier = Modifier.padding(horizontal = Spacing.lg),
        )

        if (state.submitError != null) {
            Text(
                text = stringResource(R.string.post_compose_save_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
        }
    }
}

@Composable
private fun TitleField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val titleStyle = MaterialTheme.typography.headlineLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
    )
    Box(modifier = modifier.fillMaxWidth()) {
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
    modifier: Modifier = Modifier,
) {
    val bodyStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp),
    ) {
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
private fun PhotosStrip(
    photos: List<PhotoSlot>,
    canAddMore: Boolean,
    onRemove: (localId: String) -> Unit,
    onAdd: () -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = PaddingValues(horizontal = Spacing.lg),
    ) {
        items(items = photos, key = { it.localId }) { slot ->
            PhotoSlotTile(slot = slot, onRemove = { onRemove(slot.localId) })
        }
        if (canAddMore) {
            item(key = "add_more") {
                AddPhotoTile(onClick = onAdd)
            }
        }
    }
}

@Composable
private fun PhotoSlotTile(slot: PhotoSlot, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .size(PhotoTileSize)
            .clip(RoundedCornerShape(PhotoTileCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        when (slot) {
            is PhotoSlot.Ready -> AsyncImage(
                model = slot.previewUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            is PhotoSlot.Uploading -> CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp),
                color = MaterialTheme.colorScheme.secondary,
                strokeWidth = 2.dp,
            )

            is PhotoSlot.Error -> Icon(
                painter = painterResource(R.drawable.ic_error),
                contentDescription = stringResource(R.string.post_compose_photo_upload_error),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp),
            )
        }
        RemovePhotoButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp),
        )
    }
}

@Composable
private fun RemovePhotoButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = stringResource(R.string.post_compose_remove_photo),
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun AddPhotoTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(PhotoTileSize)
            .clip(RoundedCornerShape(PhotoTileCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 36.sp,
            fontWeight = FontWeight.Light,
        )
    }
}

private val PhotoTileSize = 96.dp
private val PhotoTileCornerRadius = 12.dp
