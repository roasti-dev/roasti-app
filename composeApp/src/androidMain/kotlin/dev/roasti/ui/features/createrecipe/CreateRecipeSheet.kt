package dev.roasti.ui.features.createrecipe

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import dev.roasti.ui.features.createrecipe.model.CreateRecipeEvent
import dev.roasti.ui.features.createrecipe.steps.BasicsStep
import dev.roasti.ui.features.createrecipe.steps.PreviewStep
import dev.roasti.ui.features.createrecipe.steps.StepsStep
import dev.roasti.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRecipeSheet(
    onDismiss: () -> Unit,
    onPublished: (success: Boolean) -> Unit,
) {
    val viewModel: CreateRecipeViewModel = koinViewModel()
    val formState by viewModel.state.collectAsStateWithLifecycle()
    val sheetNavController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when(event) {
                is CreateRecipeEvent.OnRequestFinished -> {
                    onPublished(event.isSuccessful)
                }
                is CreateRecipeEvent.OnImageUploadFailed -> {
                    snackbarHostState.showSnackbar("Failed to upload image. Please try again.")
                }
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
        confirmValueChange = { newValue ->
            if (newValue == SheetValue.Hidden && formState.isDirty) {
                showDiscardDialog = true
                false
            } else {
                true
            }
        },
    )

    val dismissSheet: () -> Unit = {
        coroutineScope.launch {
            sheetState.hide()
            onDismiss()
        }
    }

    val requestDiscard: () -> Unit = {
        if (formState.isDirty) showDiscardDialog = true else dismissSheet()
    }

    val resetAndDismiss: () -> Unit = {
        viewModel.reset()
        sheetNavController.popBackStack(SheetStep.Basics, inclusive = false)
        dismissSheet()
    }

    val currentBackStackEntry by sheetNavController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val expandInteractionSource = remember { MutableInteractionSource() }

    ModalBottomSheet(
        onDismissRequest = {
            if (formState.isDirty) showDiscardDialog = true else onDismiss()
        },
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0) },
        modifier = Modifier.statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Create Recipe", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = requestDiscard) {
                Text("✕", style = MaterialTheme.typography.titleLarge)
            }
        }

        // Step indicator
        StepIndicator(
            currentRoute = currentRoute,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xs),
        )

        HorizontalDivider()

        SnackbarHost(snackbarHostState)

        // NavHost with steps + expand overlay
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            NavHost(
                navController = sheetNavController,
                startDestination = SheetStep.Basics,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.lg),
                enterTransition = { slideInHorizontally { it } + fadeIn() },
                exitTransition = { slideOutHorizontally { -it } + fadeOut() },
                popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
                popExitTransition = { slideOutHorizontally { it } + fadeOut() },
            ) {
                composable(SheetStep.Basics) {
                    BasicsStep(
                        state = formState,
                        onNameChange = viewModel::updateName,
                        onBrewMethodChange = viewModel::updateBrewMethod,
                        onBeansChange = viewModel::updateBeans,
                        onDifficultyChange = viewModel::updateDifficulty,
                        onRoastLevelChange = viewModel::updateRoastLevel,
                        onDescriptionChange = viewModel::updateDescription,
                        onUploadImage = viewModel::uploadImage,
                        onContinue = { sheetNavController.navigate(SheetStep.Steps) },
                        onDiscardRequest = { showDiscardDialog = true },
                        onDismiss = dismissSheet,
                    )
                }
                composable(SheetStep.Steps) {
                    StepsStep(
                        state = formState,
                        onAddStep = viewModel::addBrewStep,
                        onRemoveStep = viewModel::removeBrewStepByIndex,
                        onUploadStepImage = viewModel::uploadBrewStepImage,
                        onBack = { sheetNavController.popBackStack() },
                        onContinue = { sheetNavController.navigate(SheetStep.Preview) },
                    )
                }
                composable(SheetStep.Preview) {
                    PreviewStep(
                        state = formState,
                        onBack = { sheetNavController.popBackStack() },
                        onUpload = { viewModel.publishRecipe() }
                    )
                }
            }

            // Transparent overlay to expand sheet on tap when partially expanded
            if (sheetState.currentValue == SheetValue.PartiallyExpanded) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = expandInteractionSource,
                            indication = null,
                        ) {
                            coroutineScope.launch { sheetState.expand() }
                        },
                )
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("Your recipe will not be saved.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        resetAndDismiss()
                    },
                ) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep editing")
                }
            },
        )
    }
}

@Composable
private fun StepIndicator(currentRoute: String?, modifier: Modifier = Modifier) {
    val steps = listOf(
        SheetStep.Basics to "1. Basics",
        SheetStep.Steps to "2. Steps",
        SheetStep.Preview to "3. Preview",
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { index, (route, label) ->
            val isActive = currentRoute == route
            AssistChip(
                onClick = {},
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                colors = if (isActive) {
                    AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        labelColor = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                },
            )
            if (index < steps.size - 1) {
                Text(
                    ">",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
