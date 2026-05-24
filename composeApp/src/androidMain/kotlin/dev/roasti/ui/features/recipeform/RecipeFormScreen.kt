package dev.roasti.ui.features.recipeform

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import dev.roasti.R
import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.RoastLevel
import dev.roasti.ui.features.recipe.mapper.labelRes
import dev.roasti.ui.features.recipeform.model.RecipeFormFields
import dev.roasti.ui.features.recipeform.model.RecipeFormStepUiModel
import dev.roasti.ui.features.recipeform.model.StepDraft
import dev.roasti.ui.theme.Sand200
import dev.roasti.ui.theme.Sand300
import dev.roasti.ui.theme.Sand500
import dev.roasti.ui.theme.Sand600
import dev.roasti.ui.theme.Sand700
import dev.roasti.ui.theme.ShapeXxl
import dev.roasti.ui.theme.Spacing
import dev.roasti.ui.uikit.picker.ChipCarouselPicker
import dev.roasti.ui.uikit.picker.ChipOption
import dev.roasti.ui.uikit.picker.TimeWheelPicker
import dev.roasti.ui.uikit.requiredLabel
import dev.roasti.ui.theme.RoastiTheme
import dev.roasti.utils.compressImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import java.util.UUID

private val HeroHeight = 220.dp
private val SectionHorizontalPadding = Spacing.xxl
private val StepShape = RoundedCornerShape(16.dp)
private val FieldShape = RoundedCornerShape(12.dp)
private val SaveButtonHeight = 56.dp
private val StepRowMinHeight = 72.dp
private val ChipMinHeight = 64.dp
private val DurationChipShape = RoundedCornerShape(10.dp)

private enum class ExpandedEnumPicker { BrewMethod, Difficulty, RoastLevel }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecipeFormScreen(
    form: RecipeFormFields,
    topBarTitle: String,
    saveButtonLabel: String,
    isDirty: Boolean,
    isCreateMode: Boolean,
    saveErrorEventTrigger: Int,
    listener: RecipeFormListener,
) {
    var showDiscardDialog by remember { mutableStateOf(false) }
    var expandedEnumPicker by remember { mutableStateOf<ExpandedEnumPicker?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val saveErrorText = stringResource(R.string.edit_recipe_save_error)

    LaunchedEffect(saveErrorEventTrigger) {
        if (saveErrorEventTrigger > 0) {
            snackbarHostState.showSnackbar(saveErrorText)
        }
    }

    BackHandler {
        if (isDirty) showDiscardDialog = true else listener.onBackClick()
    }

    val context = LocalContext.current
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bytes = compressImage(context.contentResolver, it)
            listener.onUploadImage("${UUID.randomUUID()}.jpg", bytes)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            FormTopBar(
                title = topBarTitle,
                onBackClick = {
                    if (isDirty) showDiscardDialog = true else listener.onBackClick()
                },
            )
        },
        snackbarHost = {
            Box(Modifier.imePadding()) {
                SnackbarHost(snackbarHostState)
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            FormBody(
                form = form,
                expandedEnumPicker = expandedEnumPicker,
                onChangeExpandedPicker = { expandedEnumPicker = it },
                onImageTap = { imageLauncher.launch("image/*") },
                listener = listener,
            )

            SavePill(
                label = saveButtonLabel,
                canSave = form.canSave,
                isSaving = form.isSaving,
                onClick = listener::onSaveClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(end = Spacing.xxl, bottom = Spacing.lg),
            )
        }
    }

    if (form.editingStep != null) {
        StepEditorSheet(
            draft = form.editingStep,
            onTitleChange = listener::onDraftTitleChange,
            onDurationChange = listener::onDraftDurationChange,
            onConfirm = listener::onCommitDraft,
            onDismiss = listener::onCancelDraft,
        )
    }

    if (showDiscardDialog) {
        DiscardDialog(
            isCreateMode = isCreateMode,
            onConfirm = {
                showDiscardDialog = false
                listener.onBackClick()
            },
            onDismiss = { showDiscardDialog = false },
        )
    }
}

@Composable
private fun FormBody(
    form: RecipeFormFields,
    expandedEnumPicker: ExpandedEnumPicker?,
    onChangeExpandedPicker: (ExpandedEnumPicker?) -> Unit,
    onImageTap: () -> Unit,
    listener: RecipeFormListener,
) {
    val listState = rememberLazyListState()
    val stepIds = remember(form.steps) { form.steps.mapTo(HashSet()) { it.id } }
    val reorderState = rememberReorderState(listState) { fromKey, toKey ->
        val from = form.steps.indexOfFirst { it.id == fromKey }
        val to = form.steps.indexOfFirst { it.id == toKey }
        if (from >= 0 && to >= 0) listener.onReorderSteps(from, to)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = SaveButtonHeight + Spacing.xxxl * 2),
    ) {
        item(key = "hero") {
            HeroImage(
                imageUrl = form.imageUrl,
                isUploading = form.isUploadingImage,
                onChange = onImageTap,
                onRemove = listener::onRemoveImage,
            )
        }
        formSectionsItems(
            form = form,
            expandedEnumPicker = expandedEnumPicker,
            onChangeExpandedPicker = onChangeExpandedPicker,
            listener = listener,
        )
        stepsItems(
            form = form,
            reorderState = reorderState,
            isSwappable = { key -> key in stepIds },
            listener = listener,
        )
    }
}

private fun LazyListScope.formSectionsItems(
    form: RecipeFormFields,
    expandedEnumPicker: ExpandedEnumPicker?,
    onChangeExpandedPicker: (ExpandedEnumPicker?) -> Unit,
    listener: RecipeFormListener,
) {
    item(key = "title") {
        Box(modifier = Modifier.padding(horizontal = SectionHorizontalPadding, vertical = Spacing.sm)) {
            TitleField(value = form.title, onChange = listener::onTitleChange)
        }
    }
    item(key = "description") {
        Box(modifier = Modifier.padding(horizontal = SectionHorizontalPadding, vertical = Spacing.sm)) {
            DescriptionField(value = form.description, onChange = listener::onDescriptionChange)
        }
    }
    item(key = "params") {
        Box(modifier = Modifier.padding(horizontal = SectionHorizontalPadding, vertical = Spacing.sm)) {
            ParametersSection(
                form = form,
                expandedPicker = expandedEnumPicker,
                onChipClick = { picker ->
                    onChangeExpandedPicker(if (expandedEnumPicker == picker) null else picker)
                },
                onBrewMethodChange = {
                    listener.onBrewMethodChange(it)
                    onChangeExpandedPicker(null)
                },
                onDifficultyChange = {
                    listener.onDifficultyChange(it)
                    onChangeExpandedPicker(null)
                },
                onRoastLevelChange = {
                    listener.onRoastLevelChange(it)
                    onChangeExpandedPicker(null)
                },
            )
        }
    }
    item(key = "beans") {
        Box(modifier = Modifier.padding(horizontal = SectionHorizontalPadding, vertical = Spacing.sm)) {
            BeansField(value = form.beans, onChange = listener::onBeansChange)
        }
    }
    item(key = "steps-header") {
        Box(
            modifier = Modifier.padding(
                horizontal = SectionHorizontalPadding,
                vertical = Spacing.md,
            ),
        ) {
            Text(
                text = stringResource(R.string.recipe_brewing_steps),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun LazyListScope.stepsItems(
    form: RecipeFormFields,
    reorderState: ReorderState,
    isSwappable: (Any) -> Boolean,
    listener: RecipeFormListener,
) {
    items(items = form.steps, key = { it.id }) { step ->
        val currentIndex = form.steps.indexOfFirst { it.id == step.id }
        if (currentIndex < 0) return@items

        val isDragging = reorderState.isDragging(step.id)
        val rowModifier = Modifier
            .zIndex(if (isDragging) 1f else 0f)
            .then(
                if (isDragging) {
                    Modifier.graphicsLayer { translationY = reorderState.draggingItemOffsetY }
                } else {
                    Modifier.animateItem()
                },
            )
            .padding(horizontal = SectionHorizontalPadding, vertical = Spacing.xs)

        StepRow(
            step = step,
            index = currentIndex,
            isDragging = isDragging,
            onTap = { listener.onOpenEditStep(currentIndex) },
            onRemove = { listener.onRemoveStep(currentIndex) },
            modifier = rowModifier,
            onDragStart = { reorderState.onDragStart(step.id) },
            onDrag = { dy -> reorderState.onDrag(dy, isSwappable) },
            onDragEnd = { reorderState.onDragEnd() },
        )
    }

    item(key = "add-step") {
        Box(
            modifier = Modifier.padding(
                horizontal = SectionHorizontalPadding,
                vertical = Spacing.md,
            ),
        ) {
            AddStepButton(onClick = listener::onOpenAddStep)
        }
    }
}

@Composable
private fun HeroImage(
    imageUrl: String?,
    isUploading: Boolean,
    onChange: () -> Unit,
    onRemove: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HeroHeight)
            .clickable(enabled = !isUploading, onClick = onChange),
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.04f),
                            Color.Black.copy(alpha = 0.16f),
                            Color.Black.copy(alpha = 0.4f),
                        ),
                    ),
                ),
        )

        if (isUploading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_image),
                        contentDescription = stringResource(R.string.edit_recipe_image_change),
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .padding(Spacing.sm)
                            .size(28.dp),
                    )
                }
            }

            if (imageUrl != null) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.md)
                        .clickable(onClick = onRemove),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.edit_recipe_image_remove),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(Spacing.sm)
                            .size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FormTopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = stringResource(R.string.back_label),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.size(Spacing.xs))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun EditableFieldBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(FieldShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
    ) {
        content()
        Icon(
            painter = painterResource(R.drawable.ic_edit),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = Spacing.xs, end = Spacing.xs)
                .size(16.dp),
        )
    }
}

@Composable
private fun TitleField(value: String, onChange: (String) -> Unit) {
    EditableFieldBox {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = requiredLabel(stringResource(R.string.edit_recipe_title_hint)),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun DescriptionField(value: String, onChange: (String) -> Unit) {
    EditableFieldBox {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = requiredLabel(stringResource(R.string.edit_recipe_description_hint)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun BeansField(value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = stringResource(R.string.recipe_beans),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        EditableFieldBox {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = stringResource(R.string.edit_recipe_beans_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }
}

@Composable
private fun ParametersSection(
    form: RecipeFormFields,
    expandedPicker: ExpandedEnumPicker?,
    onChipClick: (ExpandedEnumPicker) -> Unit,
    onBrewMethodChange: (BrewMethod) -> Unit,
    onDifficultyChange: (Difficulty) -> Unit,
    onRoastLevelChange: (RoastLevel) -> Unit,
) {
    Column(
        modifier = Modifier.animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            EnumChip(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.recipe_brew_method),
                value = stringResource(form.brewMethod.labelRes()),
                isActive = expandedPicker == ExpandedEnumPicker.BrewMethod,
                onClick = { onChipClick(ExpandedEnumPicker.BrewMethod) },
            )
            EnumChip(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.recipe_difficulty),
                value = stringResource(form.difficulty.labelRes()),
                isActive = expandedPicker == ExpandedEnumPicker.Difficulty,
                onClick = { onChipClick(ExpandedEnumPicker.Difficulty) },
            )
            EnumChip(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.recipe_roast_level),
                value = if (form.roastLevel == RoastLevel.NONE) {
                    stringResource(R.string.recipe_missing_value)
                } else {
                    stringResource(form.roastLevel.labelRes())
                },
                isActive = expandedPicker == ExpandedEnumPicker.RoastLevel,
                onClick = { onChipClick(ExpandedEnumPicker.RoastLevel) },
            )
        }

        if (expandedPicker != null) {
            ExpandedPickerSection(
                picker = expandedPicker,
                form = form,
                onBrewMethodChange = onBrewMethodChange,
                onDifficultyChange = onDifficultyChange,
                onRoastLevelChange = onRoastLevelChange,
            )
        }
    }
}

@Composable
private fun EnumChip(
    title: String,
    value: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val titleColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val valueColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .heightIn(min = ChipMinHeight)
            .clip(MaterialTheme.shapes.large)
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ExpandedPickerSection(
    picker: ExpandedEnumPicker,
    form: RecipeFormFields,
    onBrewMethodChange: (BrewMethod) -> Unit,
    onDifficultyChange: (Difficulty) -> Unit,
    onRoastLevelChange: (RoastLevel) -> Unit,
) {
    when (picker) {
        ExpandedEnumPicker.BrewMethod -> {
            val options = BrewMethod.entries
                .filterNot { it == BrewMethod.NONE }
                .map { ChipOption(it, stringResource(it.labelRes())) }
            ChipCarouselPicker(
                options = options,
                selected = form.brewMethod,
                onSelect = onBrewMethodChange,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        ExpandedEnumPicker.Difficulty -> {
            val options = Difficulty.entries
                .map { ChipOption(it, stringResource(it.labelRes())) }
            ChipCarouselPicker(
                options = options,
                selected = form.difficulty,
                onSelect = onDifficultyChange,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        ExpandedEnumPicker.RoastLevel -> {
            val options = RoastLevel.entries
                .filterNot { it == RoastLevel.NONE }
                .map { value ->
                    ChipOption(value, stringResource(value.labelRes()), fillTint = roastTint(value))
                }
            ChipCarouselPicker(
                options = options,
                selected = form.roastLevel,
                onSelect = onRoastLevelChange,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StepRow(
    step: RecipeFormStepUiModel,
    index: Int,
    isDragging: Boolean,
    onTap: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (dragDelta: Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val elevation by animateFloatAsState(
        targetValue = if (isDragging) 8f else 0f,
        label = "stepElevation",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = StepRowMinHeight)
            .shadow(elevation.dp, StepShape)
            .clip(StepShape)
            .background(
                if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh
                else MaterialTheme.colorScheme.surfaceContainer
            )
            .clickable(onClick = onTap),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DragHandle(
                modifier = Modifier.pointerInput(step.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDragStart()
                        },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.y)
                        },
                    )
                },
            )
            StepNumberCircle(number = index + 1)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                step.durationSeconds?.let { duration ->
                    StepDurationChip(duration = formatStepDuration(duration))
                }
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f))
                    .clickable(onClick = onRemove)
                    .padding(Spacing.sm),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_trash),
                    contentDescription = stringResource(R.string.edit_recipe_step_delete),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun DragHandle(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_drag_handle),
        contentDescription = stringResource(R.string.edit_recipe_step_drag_handle),
        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = modifier
            .padding(2.dp)
            .size(28.dp),
    )
}

@Composable
private fun StepNumberCircle(number: Int) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StepDurationChip(duration: String) {
    Box(
        modifier = Modifier
            .clip(DurationChipShape)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
    ) {
        Text(
            text = duration,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun AddStepButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = StepShape,
    ) {
        Text(
            text = stringResource(R.string.edit_recipe_add_step),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun SavePill(
    label: String,
    canSave: Boolean,
    isSaving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = canSave,
        modifier = modifier
            .defaultMinSize(minWidth = 140.dp)
            .height(SaveButtonHeight),
        shape = ShapeXxl,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        contentPadding = PaddingValues(horizontal = Spacing.xxl),
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepEditorSheet(
    draft: StepDraft,
    onTitleChange: (String) -> Unit,
    onDurationChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val hideThen: (() -> Unit) -> Unit = { action ->
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        StepEditorSheetContent(
            draft = draft,
            onTitleChange = onTitleChange,
            onDurationChange = onDurationChange,
            onConfirm = { hideThen(onConfirm) },
            onDismiss = { hideThen(onDismiss) },
        )
    }
}

@Composable
private fun StepEditorSheetContent(
    draft: StepDraft,
    onTitleChange: (String) -> Unit,
    onDurationChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xxl)
            .navigationBarsPadding()
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Text(
            text = if (draft.editingIndex != null) {
                stringResource(R.string.edit_recipe_step_edit_title)
            } else {
                stringResource(R.string.edit_recipe_step_add_title)
            },
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        )

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = requiredLabel(stringResource(R.string.edit_recipe_step_title_label)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(FieldShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = Spacing.md, vertical = Spacing.md),
            ) {
                BasicTextField(
                    value = draft.title,
                    onValueChange = onTitleChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box {
                            if (draft.title.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.edit_recipe_step_title_hint),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                )
                            }
                            inner()
                        }
                    },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = stringResource(R.string.edit_recipe_step_duration),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TimeWheelPicker(
                totalSeconds = draft.durationSeconds,
                onTotalSecondsChange = onDurationChange,
                minuteStep = 1,
                secondStep = 5,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(SaveButtonHeight),
                shape = ShapeXxl,
            ) {
                Text(
                    text = stringResource(R.string.edit_recipe_discard_confirm),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Button(
                onClick = onConfirm,
                enabled = draft.canConfirm,
                modifier = Modifier.weight(1f).height(SaveButtonHeight),
                shape = ShapeXxl,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.edit_recipe_step_done),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))
    }
}

@Composable
private fun DiscardDialog(
    isCreateMode: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val titleRes = if (isCreateMode) {
        R.string.edit_recipe_discard_create_title
    } else {
        R.string.edit_recipe_discard_title
    }
    val messageRes = if (isCreateMode) {
        R.string.edit_recipe_discard_create_message
    } else {
        R.string.edit_recipe_discard_message
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = { Text(stringResource(messageRes)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.edit_recipe_discard_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.edit_recipe_keep_editing))
            }
        },
    )
}

private fun roastTint(level: RoastLevel): Color = when (level) {
    RoastLevel.Light -> Sand200
    RoastLevel.MediumLight -> Sand300
    RoastLevel.Medium -> Sand500
    RoastLevel.MediumDark -> Sand600
    RoastLevel.Dark -> Sand700
    RoastLevel.NONE -> Color.Transparent
}

internal fun formatStepDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun rememberReorderState(
    listState: LazyListState,
    onSwap: (fromKey: Any, toKey: Any) -> Unit,
): ReorderState {
    val onSwapState = rememberUpdatedState(onSwap)
    val scope = rememberCoroutineScope()
    return remember(listState) {
        ReorderState(listState, scope) { from, to -> onSwapState.value(from, to) }
    }
}

private class ReorderState(
    private val listState: LazyListState,
    private val scope: CoroutineScope,
    private val onSwap: (Any, Any) -> Unit,
) {
    var draggingKey: Any? by mutableStateOf(null)
        private set
    private var draggedDelta: Float by mutableFloatStateOf(0f)
    private var initialOffset: Float by mutableFloatStateOf(0f)
    private var settleAnim: Animatable<Float, AnimationVector1D>? by mutableStateOf(null)
    private var settleJob: Job? = null

    private val draggingItemInfo: LazyListItemInfo?
        get() = draggingKey?.let { key ->
            listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
        }

    val draggingItemOffsetY: Float
        get() {
            settleAnim?.let { return it.value }
            val item = draggingItemInfo ?: return 0f
            return (initialOffset + draggedDelta) - item.offset
        }

    fun isDragging(key: Any): Boolean = draggingKey == key

    fun onDragStart(key: Any) {
        settleJob?.cancel()
        settleJob = null
        settleAnim = null
        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return
        draggingKey = key
        initialOffset = item.offset.toFloat()
        draggedDelta = 0f
    }

    fun onDrag(dy: Float, isSwappable: (Any) -> Boolean) {
        draggedDelta += dy
        val item = draggingItemInfo ?: return
        val draggedTop = item.offset + draggingItemOffsetY
        val draggedMid = draggedTop + item.size / 2f
        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { other ->
            other.index != item.index &&
                isSwappable(other.key) &&
                draggedMid.toInt() in other.offset..(other.offset + other.size)
        } ?: return
        onSwap(item.key, target.key)
    }

    fun onDragEnd() {
        val startValue = draggingItemOffsetY
        if (startValue == 0f) {
            clearDragState()
            return
        }
        val anim = Animatable(startValue)
        settleAnim = anim
        settleJob = scope.launch {
            try {
                anim.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
            } finally {
                if (settleAnim === anim) {
                    settleAnim = null
                    clearDragState()
                }
            }
        }
    }

    private fun clearDragState() {
        draggingKey = null
        draggedDelta = 0f
        initialOffset = 0f
    }
}

private object PreviewListener : RecipeFormListener {
    override fun onBackClick() = Unit
    override fun onSaveClick() = Unit
    override fun onTitleChange(value: String) = Unit
    override fun onDescriptionChange(value: String) = Unit
    override fun onBeansChange(value: String) = Unit
    override fun onBrewMethodChange(value: BrewMethod) = Unit
    override fun onDifficultyChange(value: Difficulty) = Unit
    override fun onRoastLevelChange(value: RoastLevel) = Unit
    override fun onUploadImage(fileName: String, bytes: ByteArray) = Unit
    override fun onRemoveImage() = Unit
    override fun onOpenAddStep() = Unit
    override fun onOpenEditStep(index: Int) = Unit
    override fun onDraftTitleChange(value: String) = Unit
    override fun onDraftDurationChange(totalSeconds: Int) = Unit
    override fun onCommitDraft() = Unit
    override fun onCancelDraft() = Unit
    override fun onRemoveStep(index: Int) = Unit
    override fun onReorderSteps(fromIndex: Int, toIndex: Int) = Unit
}

@Preview(name = "Empty (Create)", showBackground = true, heightDp = 900)
@Composable
private fun RecipeFormScreenEmptyPreview() {
    RoastiTheme {
        RecipeFormScreen(
            form = RecipeFormFields(),
            topBarTitle = "Create recipe",
            saveButtonLabel = "Create",
            isDirty = false,
            isCreateMode = true,
            saveErrorEventTrigger = 0,
            listener = PreviewListener,
        )
    }
}

@Preview(name = "Filled (Edit)", showBackground = true, heightDp = 1200)
@Composable
private fun RecipeFormScreenFilledPreview() {
    RoastiTheme {
        RecipeFormScreen(
            form = RecipeFormFields(
                title = "V60 Citrus Bloom",
                description = "Bright, washed Ethiopian recipe with extended bloom for clarity.",
                brewMethod = BrewMethod.V60,
                difficulty = Difficulty.Medium,
                roastLevel = RoastLevel.Light,
                beans = "Yirgacheffe 15g, medium-fine grind",
                steps = listOf(
                    RecipeFormStepUiModel(
                        title = "Bloom",
                        durationSeconds = 45,
                    ),
                    RecipeFormStepUiModel(
                        title = "First pour to 150g",
                        durationSeconds = 30,
                    ),
                    RecipeFormStepUiModel(
                        title = "Second pour to 250g",
                        durationSeconds = 40,
                    ),
                    RecipeFormStepUiModel(
                        title = "Drawdown",
                        durationSeconds = 60,
                    ),
                ),
            ),
            topBarTitle = "Edit recipe",
            saveButtonLabel = "Save",
            isDirty = true,
            isCreateMode = false,
            saveErrorEventTrigger = 0,
            listener = PreviewListener,
        )
    }
}

@Preview(name = "Uploading image", showBackground = true, heightDp = 900)
@Composable
private fun RecipeFormScreenUploadingPreview() {
    RoastiTheme {
        RecipeFormScreen(
            form = RecipeFormFields(
                title = "Aeropress inverted",
                description = "Quick, dense cup.",
                isUploadingImage = true,
            ),
            topBarTitle = "Create recipe",
            saveButtonLabel = "Create",
            isDirty = true,
            isCreateMode = true,
            saveErrorEventTrigger = 0,
            listener = PreviewListener,
        )
    }
}
