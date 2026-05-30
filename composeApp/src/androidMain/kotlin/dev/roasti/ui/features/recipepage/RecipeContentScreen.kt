package dev.roasti.ui.features.recipepage

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import dev.roasti.R
import dev.roasti.core.utils.imageUrl
import dev.roasti.ui.features.recipelist.components.LikeButton
import dev.roasti.ui.features.recipepage.model.RecipeDetailsUiModel
import dev.roasti.ui.features.recipepage.model.RecipeStepUiModel
import dev.roasti.ui.theme.RoastiTheme
import dev.roasti.ui.theme.Spacing
import dev.roasti.ui.uikit.ActionButtonPrimary
import dev.roasti.ui.uikit.AppIcons
import dev.roasti.ui.uikit.AsyncImagePreviewProvider
import dev.roasti.ui.uikit.AuthorSubtitle
import dev.roasti.ui.uikit.OverflowMenuItem
import dev.roasti.ui.uikit.SectionHeader
import dev.roasti.ui.uikit.TopBarOverflowMenu
import dev.roasti.ui.uikit.metric.MetricEntry
import dev.roasti.ui.uikit.metric.MetricsGrid
import dev.roasti.ui.uikit.state.ContentScaffold
import dev.roasti.ui.uikit.timeline.TimelineItem
import dev.roasti.ui.util.userAvatarSharedElementModifier
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft

private val StickyTitleThresholdDp = 80.dp
private val StepNumberSize = 28.dp
private val StepDurationShape = RoundedCornerShape(10.dp)
private val RecipeHeroHeight = 220.dp
private val RecipeHeroIconSize = 132.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RecipeContentRoute(
    id: String,
    onBackClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onStartBrewing: (startStep: Int) -> Unit = {},
    onAuthorClick: (userId: String, username: String, avatarTag: String?) -> Unit = { _, _, _ -> },
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val viewModel: RecipeContentViewModel = koinViewModel(parameters = { parametersOf(id) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showRemoveDialogConfirmation: Boolean by remember { mutableStateOf(false) }

    if (showRemoveDialogConfirmation) {
        RemoveConfirmationDialog(
            onConfirmRemove = {
                viewModel.onRemoveRecipe()
                showRemoveDialogConfirmation = false
            },
            onDismiss = { showRemoveDialogConfirmation = false },
        )
    }

    LaunchedEffect(Unit) {
        viewModel.navEvents.collectLatest { event ->
            when (event) {
                RecipeContentNavEvent.NavigateBack -> onBackClick()
            }
        }
    }

    ContentScaffold(
        state = state,
        onRetry = viewModel::retry,
        events = viewModel.events,
    ) { recipe ->
        RecipeContentBody(
            recipe = recipe,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            onBackClick = onBackClick,
            onEditClick = onEditClick,
            onRemoveClick = { showRemoveDialogConfirmation = true },
            onLikeClick = viewModel::toggleLike,
            onStepClick = onStartBrewing,
            onAuthorClick = onAuthorClick,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun RecipeContentBody(
    recipe: RecipeDetailsUiModel,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onBackClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onRemoveClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onStepClick: (stepIndex: Int) -> Unit = {},
    onAuthorClick: (userId: String, username: String, avatarTag: String?) -> Unit = { _, _, _ -> },
) {
    val scrollState = rememberScrollState()
    val thresholdPx = with(LocalDensity.current) { StickyTitleThresholdDp.toPx() }
    val showStickyTitle by remember(thresholdPx) {
        derivedStateOf { scrollState.value > thresholdPx }
    }

    val stepModifiers = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        recipe.steps.indices.map { index ->
            with(sharedTransitionScope) {
                Modifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "brew_step_$index"),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
        }
    } else emptyList()

    val authorAvatarModifier = userAvatarSharedElementModifier(
        tag = if (recipe.author != null) "recipe_${recipe.id}" else null,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            RecipeTopBar(
                title = recipe.title,
                isLiked = recipe.isLiked,
                likesCount = recipe.likesCount,
                showOverflowMenu = recipe.isOwner,
                showStickyTitle = showStickyTitle,
                onBackClick = onBackClick,
                onLikeClick = onLikeClick,
                onEditClick = onEditClick,
                onRemoveClick = onRemoveClick,
            )
        },
        bottomBar = {
            RecipeBottomBar(
                enabled = recipe.steps.isNotEmpty(),
                onClick = { onStepClick(0) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .verticalScroll(scrollState),
        ) {
            RecipeHero(
                imageUrl = recipe.imageUrl,
                brewMethodIconRes = recipe.brewMethodIconRes,
            )
            RecipeHeaderSection(
                recipe = recipe,
                authorAvatarModifier = authorAvatarModifier,
                onAuthorClick = onAuthorClick,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            RecipeMetricsSection(recipe = recipe)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            RecipeStepsSection(
                steps = recipe.steps,
                stepModifiers = stepModifiers,
            )
            Spacer(Modifier.height(Spacing.xxxl + innerPadding.calculateBottomPadding()))
        }
    }
}

@Composable
private fun RecipeTopBar(
    title: String,
    isLiked: Boolean,
    likesCount: Int,
    showOverflowMenu: Boolean,
    showStickyTitle: Boolean,
    onBackClick: () -> Unit,
    onLikeClick: () -> Unit,
    onEditClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    val background = MaterialTheme.colorScheme.background
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (showStickyTitle) background else background.copy(alpha = 0f))
            .statusBarsPadding()
            .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = AppIcons.Regular.ArrowLeft,
                contentDescription = stringResource(R.string.back_label),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        val stickyTitleAlpha by animateFloatAsState(
            targetValue = if (showStickyTitle) 1f else 0f,
            label = "stickyTitleAlpha",
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .alpha(stickyTitleAlpha)
                    .padding(horizontal = Spacing.sm),
            )
        }
        LikeButton(
            isLiked = isLiked,
            likesCount = likesCount,
            onClick = onLikeClick,
            modifier = Modifier.padding(end = Spacing.sm),
        )
        if (showOverflowMenu) {
            TopBarOverflowMenu(
                items = listOf(
                    OverflowMenuItem(
                        label = stringResource(R.string.recipe_edit),
                        onClick = onEditClick,
                    ),
                    OverflowMenuItem(
                        label = stringResource(R.string.recipe_remove),
                        onClick = onRemoveClick,
                        isDestructive = true,
                    ),
                ),
                contentDescription = stringResource(R.string.recipe_owner_menu_more),
            )
        }
    }
}

@Composable
private fun RecipeHero(
    imageUrl: String?,
    @DrawableRes brewMethodIconRes: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(RecipeHeroHeight)
            .background(MaterialTheme.colorScheme.tertiaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Image(
                painter = painterResource(brewMethodIconRes),
                contentDescription = null,
                modifier = Modifier.size(RecipeHeroIconSize),
            )
        }
    }
}

@Composable
private fun RecipeHeaderSection(
    recipe: RecipeDetailsUiModel,
    authorAvatarModifier: Modifier = Modifier,
    onAuthorClick: (userId: String, username: String, avatarTag: String?) -> Unit = { _, _, _ -> },
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xxl, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = recipe.title,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        recipe.author?.let { author ->
            AuthorSubtitle(
                imageUrl = author.avatarId?.let { imageUrl(it) },
                name = author.username,
                avatarModifier = authorAvatarModifier,
                onClick = {
                    onAuthorClick(author.id, author.username, "recipe_${recipe.id}")
                },
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = recipe.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecipeMetricsSection(recipe: RecipeDetailsUiModel) {
    val brewMethodIcon = painterResource(R.drawable.ic_coffee)
    val timeIcon = painterResource(R.drawable.ic_clock)
    val difficultyIcon = painterResource(R.drawable.ic_bar_chart)
    val beansIcon = painterResource(R.drawable.ic_coffee_bean)
    val roastIcon = painterResource(R.drawable.ic_flame)

    val main = buildList {
        add(
            MetricEntry(
                icon = brewMethodIcon,
                label = stringResource(R.string.recipe_brew_method),
                value = stringResource(recipe.brewMethodLabelRes),
            )
        )
        recipe.totalDurationSeconds?.let { totalSeconds ->
            add(
                MetricEntry(
                    icon = timeIcon,
                    label = stringResource(R.string.recipe_time),
                    value = formatTotalDuration(totalSeconds),
                )
            )
        }
    }
    val others = buildList {
        add(
            MetricEntry(
                icon = difficultyIcon,
                label = stringResource(R.string.recipe_difficulty),
                value = stringResource(recipe.difficultyLabelRes),
            )
        )
        recipe.beans?.takeIf { it.isNotBlank() }?.let { beans ->
            add(
                MetricEntry(
                    icon = beansIcon,
                    label = stringResource(R.string.recipe_beans),
                    value = beans,
                )
            )
        }
        recipe.roastLevelLabelRes?.let { res ->
            add(
                MetricEntry(
                    icon = roastIcon,
                    label = stringResource(R.string.recipe_roast_level),
                    value = stringResource(res),
                )
            )
        }
    }

    MetricsGrid(
        main = main,
        others = others,
        modifier = Modifier.padding(horizontal = Spacing.xxl, vertical = Spacing.xl),
    )
}

@Composable
private fun RecipeStepsSection(
    steps: List<RecipeStepUiModel>,
    stepModifiers: List<Modifier> = emptyList(),
) {
    Column(
        modifier = Modifier.padding(horizontal = Spacing.xxl, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        SectionHeader(text = stringResource(R.string.recipe_brewing_steps))
        Column {
            steps.forEachIndexed { index, step ->
                TimelineItem(
                    isLast = index == steps.lastIndex,
                    modifier = stepModifiers.getOrElse(index) { Modifier },
                    leading = {
                        Box(
                            modifier = Modifier
                                .size(StepNumberSize)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    },
                    content = {
                        StepContent(step = step)
                    },
                )
            }
        }
    }
}

@Composable
private fun StepContent(step: RecipeStepUiModel) {
    Column(
        modifier = Modifier.padding(bottom = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = step.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        step.durationSeconds?.let { duration ->
            StepDurationChip(duration = formatStepDuration(duration))
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
private fun RecipeBottomBar(
    enabled: Boolean,
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
        ActionButtonPrimary(
            onClick = onClick,
            text = stringResource(R.string.recipe_start_brewing),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = Spacing.xxl,
                    top = 0.dp,
                    end = Spacing.xxl,
                    bottom = Spacing.lg,
                ),
            enabled = enabled,
        )
    }
}

@Composable
private fun RemoveConfirmationDialog(onConfirmRemove: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_confirm_title)) },
        confirmButton = {
            TextButton(onClick = onConfirmRemove) {
                Text(
                    text = stringResource(R.string.dialog_remove_label),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_dismiss_label))
            }
        },
    )
}

@Composable
private fun formatTotalDuration(totalSeconds: Int): String {
    val totalMinutes = totalSeconds / 60
    val days = totalMinutes / (60 * 24)
    val hours = (totalMinutes % (60 * 24)) / 60
    val minutes = totalMinutes % 60
    return when {
        days > 0 && hours > 0 -> stringResource(R.string.recipe_duration_day, days) +
            " " + stringResource(R.string.recipe_duration_hour, hours)
        days > 0 -> stringResource(R.string.recipe_duration_day, days)
        hours > 0 && minutes > 0 -> stringResource(R.string.recipe_duration_hour, hours) +
            " " + stringResource(R.string.recipe_duration_min, minutes)
        hours > 0 -> stringResource(R.string.recipe_duration_hour, hours)
        else -> stringResource(R.string.recipe_duration_min, minutes.coerceAtLeast(0))
    }
}

private fun formatStepDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun RecipeContentScreenPreview() {
    RoastiTheme {
        AsyncImagePreviewProvider {
            RecipeContentBody(
                recipe = RecipeDetailsUiModel(
                    id = "aeropress-inverted",
                    title = "Aeropress Inverted",
                    description = "Clean and sweet cup with balanced acidity and a compact recipe flow that stays readable even with multiple brewing steps.",
                    imageUrl = null,
                    brewMethodLabelRes = R.string.recipe_brew_method_aeropress,
                    brewMethodIconRes = R.drawable.ic_brew_aeropress,
                    difficultyLabelRes = R.string.recipe_difficulty_medium,
                    roastLevelLabelRes = R.string.recipe_roast_level_medium,
                    beans = "Colombian Supremo",
                    isLiked = true,
                    likesCount = 42,
                    totalDurationSeconds = 180,
                    isOwner = true,
                    steps = listOf(
                        RecipeStepUiModel(
                            order = 1,
                            title = "Setup Inverted",
                            durationSeconds = 30,
                        ),
                        RecipeStepUiModel(
                            order = 2,
                            title = "Add Coffee",
                            durationSeconds = 15,
                        ),
                        RecipeStepUiModel(
                            order = 3,
                            title = "First Pour",
                            durationSeconds = 30,
                        ),
                        RecipeStepUiModel(
                            order = 4,
                            title = "Stir",
                            durationSeconds = 10,
                        ),
                        RecipeStepUiModel(
                            order = 5,
                            title = "Top Up",
                            durationSeconds = 35,
                        ),
                        RecipeStepUiModel(
                            order = 6,
                            title = "Flip And Press",
                            durationSeconds = 60,
                        ),
                    ),
                )
            )
        }
    }
}
