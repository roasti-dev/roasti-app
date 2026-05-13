package dev.roasti.ui.features.recipeform

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.Camera
import com.adamglin.phosphoricons.regular.Check
import com.adamglin.phosphoricons.regular.FloppyDiskBack
import com.adamglin.phosphoricons.regular.Pencil
import com.adamglin.phosphoricons.regular.TrashSimple
import dev.roasti.R
import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.RoastLevel
import dev.roasti.ui.features.recipe.mapper.labelRes
import dev.roasti.ui.features.recipeform.model.ActiveStepSheet
import dev.roasti.ui.features.recipeform.model.RecipeFormFields
import dev.roasti.ui.features.recipeform.model.RecipeFormStepUiModel
import dev.roasti.ui.theme.ShapeXxl
import dev.roasti.ui.theme.Spacing
import dev.roasti.ui.uikit.AppIcons
import dev.roasti.ui.uikit.RoastiBottomSheet
import dev.roasti.utils.compressImage
import java.util.UUID

private const val IconCheck = "✓"

internal val RecipeFormHeaderHeight = 220.dp
internal val RecipeFormHeaderOverlap = 40.dp
private val MetaChipShape = RoundedCornerShape(18.dp)
private val StepNumberSize = 28.dp
private val StepDurationShape = RoundedCornerShape(10.dp)
private val HeaderActionButtonHeight = 40.dp
private val PrimaryButtonHeight = 56.dp
internal val RecipeFormContentShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
private val HeaderActionShape = RoundedCornerShape(20.dp)

private sealed class EnumSheet {
    object BrewMethod : EnumSheet()
    object Difficulty : EnumSheet()
    object RoastLevel : EnumSheet()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecipeFormScreen(
    form: RecipeFormFields,
    saveButtonLabel: String,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onBeansChange: (String) -> Unit,
    onBrewMethodChange: (BrewMethod) -> Unit,
    onDifficultyChange: (Difficulty) -> Unit,
    onRoastLevelChange: (RoastLevel) -> Unit,
    onUploadImage: (String, ByteArray) -> Unit,
    onEditStep: (Int) -> Unit,
    onDeleteStep: (Int) -> Unit,
    onAddStep: () -> Unit,
    onActiveStepTitleChange: (String) -> Unit,
    onActiveStepDurationMinutesChange: (String) -> Unit,
    onActiveStepDurationSecondsChange: (String) -> Unit,
    onConfirmStepEdit: () -> Unit,
    onCancelStepEdit: () -> Unit,
) {
    var showDiscardDialog by remember { mutableStateOf(false) }
    var visibleEnumSheet by remember { mutableStateOf<EnumSheet?>(null) }

    BackHandler { showDiscardDialog = true }

    val context = LocalContext.current
    val imageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val bytes = compressImage(context.contentResolver, it)
                onUploadImage("${UUID.randomUUID()}.jpg", bytes)
            }
        }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            RecipeFormBottomBar(
                isSaving = form.isSaving,
                saveError = form.saveError,
                canSave = form.canSave,
                saveButtonLabel = saveButtonLabel,
                onClick = onSaveClick,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            RecipeFormHeaderImage(
                imageUrl = form.imageUrl,
                isUploading = form.isUploadingImage,
            )
            RecipeFormContentList(
                form = form,
                bottomContentPadding = innerPadding.calculateBottomPadding(),
                onImageTap = { imageLauncher.launch("image/*") },
                onTitleChange = onTitleChange,
                onDescriptionChange = onDescriptionChange,
                onBeansChange = onBeansChange,
                onBrewMethodChipClick = { visibleEnumSheet = EnumSheet.BrewMethod },
                onDifficultyChipClick = { visibleEnumSheet = EnumSheet.Difficulty },
                onRoastLevelChipClick = { visibleEnumSheet = EnumSheet.RoastLevel },
                onEditStep = onEditStep,
                onDeleteStep = onDeleteStep,
                onAddStep = onAddStep,
                modifier = Modifier.fillMaxSize(),
            )
            RecipeFormTopBar(
                isSaving = form.isSaving,
                onBackClick = { showDiscardDialog = true },
                onSaveClick = onSaveClick,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }

    // Enum pickers
    val brewMethodOptions = BrewMethod.entries
        .filterNot { it == BrewMethod.NONE }
        .map { it to stringResource(it.labelRes()) }
    val difficultyOptions = Difficulty.entries.map { it to stringResource(it.labelRes()) }
    val roastLevelOptions = RoastLevel.entries
        .filterNot { it == RoastLevel.NONE }
        .map { it to stringResource(it.labelRes()) }

    when (visibleEnumSheet) {
        EnumSheet.BrewMethod -> OptionPickerBottomSheet(
            title = stringResource(R.string.recipe_brew_method),
            options = brewMethodOptions,
            selected = form.brewMethod,
            onSelect = { onBrewMethodChange(it); visibleEnumSheet = null },
            onDismiss = { visibleEnumSheet = null },
        )

        EnumSheet.Difficulty -> OptionPickerBottomSheet(
            title = stringResource(R.string.recipe_difficulty),
            options = difficultyOptions,
            selected = form.difficulty,
            onSelect = { onDifficultyChange(it); visibleEnumSheet = null },
            onDismiss = { visibleEnumSheet = null },
        )

        EnumSheet.RoastLevel -> OptionPickerBottomSheet(
            title = stringResource(R.string.recipe_roast_level),
            options = roastLevelOptions,
            selected = form.roastLevel,
            onSelect = { onRoastLevelChange(it); visibleEnumSheet = null },
            onDismiss = { visibleEnumSheet = null },
        )

        null -> Unit
    }

    // Step edit sheet
    form.activeStepSheet?.let { sheet ->
        StepEditBottomSheet(
            sheet = sheet,
            onTitleChange = onActiveStepTitleChange,
            onDurationMinutesChange = onActiveStepDurationMinutesChange,
            onDurationSecondsChange = onActiveStepDurationSecondsChange,
            onConfirm = onConfirmStepEdit,
            onDismiss = onCancelStepEdit,
        )
    }

    // Discard dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.edit_recipe_discard_title)) },
            text = { Text(stringResource(R.string.edit_recipe_discard_message)) },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog = false; onBackClick() }) {
                    Text(
                        text = stringResource(R.string.edit_recipe_discard_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.edit_recipe_keep_editing))
                }
            },
        )
    }
}

@Composable
internal fun RecipeFormHeaderImage(
    imageUrl: String?,
    isUploading: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(RecipeFormHeaderHeight),
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
                            MaterialTheme.colorScheme.background.copy(alpha = 0.04f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                        )
                    )
                )
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
                        imageVector = AppIcons.Regular.Camera,
                        contentDescription = "choose photo",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun RecipeFormTopBar(
    isSaving: Boolean,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {

        ActionButton(
            image = AppIcons.Regular.ArrowLeft,
            onClick = onBackClick,
            contentDescription = stringResource(R.string.back_label)
        )

        ActionButton(
            image = AppIcons.Regular.FloppyDiskBack,
            onClick = onSaveClick,
            contentDescription = stringResource(R.string.back_label),
            enabled = !isSaving,
        )
    }
}

@Composable
private fun ActionButton(
    image: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    tint: Color = MaterialTheme.colorScheme.tertiary
) {
    IconButton(onClick, modifier, enabled = enabled) {
        Icon(
            imageVector = image,
            contentDescription = contentDescription,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp),
            tint = tint
        )
    }
}

@Composable
internal fun RecipeFormContentList(
    form: RecipeFormFields,
    bottomContentPadding: Dp,
    onImageTap: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onBeansChange: (String) -> Unit,
    onBrewMethodChipClick: () -> Unit,
    onDifficultyChipClick: () -> Unit,
    onRoastLevelChipClick: () -> Unit,
    onEditStep: (Int) -> Unit,
    onDeleteStep: (Int) -> Unit,
    onAddStep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.imePadding(),
        contentPadding = PaddingValues(bottom = Spacing.xxxl + bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        item {
            Box(
                modifier = Modifier
                    .height(RecipeFormHeaderHeight - RecipeFormHeaderOverlap)
                    .fillMaxWidth()
                    .clickable(enabled = !form.isUploadingImage, onClick = onImageTap),
            )
        }
        item {
            RecipeFormMainContent(
                form = form,
                onTitleChange = onTitleChange,
                onDescriptionChange = onDescriptionChange,
                onBeansChange = onBeansChange,
                onBrewMethodChipClick = onBrewMethodChipClick,
                onDifficultyChipClick = onDifficultyChipClick,
                onRoastLevelChipClick = onRoastLevelChipClick,
                onEditStep = onEditStep,
                onDeleteStep = onDeleteStep,
                onAddStep = onAddStep,
            )
        }
    }
}

@Composable
private fun RecipeFormMainContent(
    form: RecipeFormFields,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onBeansChange: (String) -> Unit,
    onBrewMethodChipClick: () -> Unit,
    onDifficultyChipClick: () -> Unit,
    onRoastLevelChipClick: () -> Unit,
    onEditStep: (Int) -> Unit,
    onDeleteStep: (Int) -> Unit,
    onAddStep: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RecipeFormContentShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Spacing.xxl, vertical = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        RecipeFormTitleField(title = form.title, onTitleChange = onTitleChange)
        RecipeFormDescriptionField(
            description = form.description,
            onDescriptionChange = onDescriptionChange
        )
        RecipeFormMetaSection(
            form = form,
            onBeansChange = onBeansChange,
            onBrewMethodChipClick = onBrewMethodChipClick,
            onDifficultyChipClick = onDifficultyChipClick,
            onRoastLevelChipClick = onRoastLevelChipClick,
        )
        RecipeFormStepsSection(
            steps = form.steps,
            onEditStep = onEditStep,
            onDeleteStep = onDeleteStep,
            onAddStep = onAddStep,
        )
    }
}

@Composable
private fun RecipeFormTitleField(title: String, onTitleChange: (String) -> Unit) {
    BasicTextField(
        value = title,
        onValueChange = onTitleChange,
        textStyle = MaterialTheme.typography.headlineLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Box {
                if (title.isEmpty()) {
                    Text(
                        text = stringResource(R.string.edit_recipe_title_hint),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun RecipeFormDescriptionField(description: String, onDescriptionChange: (String) -> Unit) {
    BasicTextField(
        value = description,
        onValueChange = onDescriptionChange,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Box {
                if (description.isEmpty()) {
                    Text(
                        text = stringResource(R.string.edit_recipe_description_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    )
                }
                innerTextField()
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeFormMetaSection(
    form: RecipeFormFields,
    onBeansChange: (String) -> Unit,
    onBrewMethodChipClick: () -> Unit,
    onDifficultyChipClick: () -> Unit,
    onRoastLevelChipClick: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxWidth(),
    ) {
        EditableEnumChip(
            title = stringResource(R.string.recipe_brew_method),
            value = stringResource(form.brewMethod.labelRes()),
            isHighlighted = true,
            onClick = onBrewMethodChipClick,
        )
        EditableEnumChip(
            title = stringResource(R.string.recipe_difficulty),
            value = stringResource(form.difficulty.labelRes()),
            onClick = onDifficultyChipClick,
        )
        if (form.roastLevel != RoastLevel.NONE) {
            EditableEnumChip(
                title = stringResource(R.string.recipe_roast_level),
                value = stringResource(form.roastLevel.labelRes()),
                onClick = onRoastLevelChipClick,
            )
        } else {
            EditableEnumChip(
                title = stringResource(R.string.recipe_roast_level),
                value = stringResource(R.string.recipe_missing_value),
                onClick = onRoastLevelChipClick,
            )
        }
        EditableBeansChip(beans = form.beans, onBeansChange = onBeansChange)
    }
}

@Composable
private fun EditableEnumChip(
    title: String,
    value: String,
    onClick: () -> Unit,
    isHighlighted: Boolean = false,
) {
    val backgroundColor = if (isHighlighted) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val titleColor = if (isHighlighted) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .clip(MetaChipShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = titleColor,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Icon(
                imageVector = AppIcons.Regular.Pencil,
                contentDescription = "edit",
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(16.dp)
            )
        }
    }
}

@Composable
private fun EditableBeansChip(beans: String, onBeansChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .clip(MetaChipShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = stringResource(R.string.recipe_beans),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BasicTextField(
            value = beans,
            onValueChange = onBeansChange,
            textStyle = MaterialTheme.typography.titleSmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.defaultMinSize(minWidth = 80.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (beans.isEmpty()) {
                        Text(
                            text = stringResource(R.string.edit_recipe_beans_hint),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
internal fun RecipeFormStepsSection(
    steps: List<RecipeFormStepUiModel>,
    onEditStep: (Int) -> Unit,
    onDeleteStep: (Int) -> Unit,
    onAddStep: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(
            text = stringResource(R.string.recipe_brewing_steps),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Column {
            steps.forEachIndexed { index, step ->
                EditableStepItem(
                    step = step,
                    stepNumber = index + 1,
                    onEditClick = { onEditStep(index) },
                    onDeleteClick = { onDeleteStep(index) },
                )
                if (index != steps.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = StepNumberSize + Spacing.md),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
        AddStepButton(onClick = onAddStep)
    }
}

@Composable
private fun EditableStepItem(
    step: RecipeFormStepUiModel,
    stepNumber: Int,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Spacing.md))
            .clickable(onClick = onEditClick)
            .padding(vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(StepNumberSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stepNumber.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (step.description.isNotBlank()) {
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            step.durationSeconds?.let { duration ->
                StepDurationChip(duration = formatStepDuration(duration))
            }
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onDeleteClick)
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.Regular.TrashSimple,
                contentDescription = "remove step",
                modifier = Modifier
                    .padding(8.dp)
                    .size(16.dp)
            )
        }
    }
}

@Composable
private fun StepDurationChip(duration: String) {
    Box(
        modifier = Modifier
            .clip(StepDurationShape)
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
        modifier = Modifier
            .fillMaxWidth()
            .height(PrimaryButtonHeight),
        shape = ShapeXxl,
    ) {
        Text(
            text = stringResource(R.string.edit_recipe_add_step),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
internal fun RecipeFormBottomBar(
    isSaving: Boolean,
    saveError: Boolean,
    canSave: Boolean,
    saveButtonLabel: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                        0.32f to MaterialTheme.colorScheme.surface.copy(alpha = 0.48f),
                        0.62f to MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                        1.0f to MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = Spacing.xxl, end = Spacing.xxl, bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (saveError) {
                Text(
                    text = stringResource(R.string.edit_recipe_save_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Button(
                onClick = onClick,
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PrimaryButtonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = ShapeXxl,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = saveButtonLabel,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> OptionPickerBottomSheet(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    RoastiBottomSheet(
        onDismiss = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = Spacing.xxl, vertical = Spacing.lg),
        )
        options.forEach { (value, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(value) }
                    .padding(horizontal = Spacing.xxl, vertical = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (value == selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (value == selected) {
                    Icon(
                        imageVector = AppIcons.Regular.Check,
                        contentDescription = "checked",
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StepEditBottomSheet(
    sheet: ActiveStepSheet,
    onTitleChange: (String) -> Unit,
    onDurationMinutesChange: (String) -> Unit,
    onDurationSecondsChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
                text = if (sheet.editingIndex != null) {
                    stringResource(R.string.edit_recipe_step_edit_title)
                } else {
                    stringResource(R.string.edit_recipe_step_add_title)
                },
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = sheet.title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.edit_recipe_step_title_label) + " *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = sheet.durationMinutes,
                    onValueChange = {
                        if (it.length <= 2 && it.all(Char::isDigit)) onDurationMinutesChange(
                            it
                        )
                    },
                    label = { Text(stringResource(R.string.edit_recipe_step_duration_min)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.width(88.dp),
                )
                OutlinedTextField(
                    value = sheet.durationSeconds,
                    onValueChange = {
                        if (it.length <= 2 && it.all(Char::isDigit)) onDurationSecondsChange(
                            it
                        )
                    },
                    label = { Text(stringResource(R.string.edit_recipe_step_duration_sec)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.width(88.dp),
                )
                Spacer(modifier = Modifier.weight(1f))
            }
            Button(
                onClick = onConfirm,
                enabled = sheet.canConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PrimaryButtonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = ShapeXxl,
            ) {
                Text(
                    text = stringResource(R.string.edit_recipe_step_save),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(Modifier.height(Spacing.md))
        }
    }
}

internal fun formatStepDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
