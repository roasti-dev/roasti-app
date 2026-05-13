package dev.roasti.ui.features.createrecipe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import dev.roasti.R
import dev.roasti.ui.features.recipeform.RecipeFormScreen

@Composable
fun CreateRecipeRoute(onBackClick: () -> Unit) {
    val viewModel: CreateRecipeScreenViewModel = koinViewModel()
    val form by viewModel.form.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                CreateRecipeScreenEvent.SaveSuccess -> onBackClick()
                CreateRecipeScreenEvent.SaveError, CreateRecipeScreenEvent.ImageUploadFailed -> Unit
            }
        }
    }

    RecipeFormScreen(
        form = form,
        saveButtonLabel = stringResource(R.string.create_recipe_create),
        onBackClick = onBackClick,
        onSaveClick = viewModel::save,
        onTitleChange = viewModel::updateTitle,
        onDescriptionChange = viewModel::updateDescription,
        onBeansChange = viewModel::updateBeans,
        onBrewMethodChange = viewModel::updateBrewMethod,
        onDifficultyChange = viewModel::updateDifficulty,
        onRoastLevelChange = viewModel::updateRoastLevel,
        onUploadImage = viewModel::uploadImage,
        onEditStep = viewModel::openEditStep,
        onDeleteStep = viewModel::removeStep,
        onAddStep = viewModel::openAddStep,
        onActiveStepTitleChange = viewModel::updateActiveStepTitle,
        onActiveStepDurationMinutesChange = viewModel::updateActiveStepDurationMinutes,
        onActiveStepDurationSecondsChange = viewModel::updateActiveStepDurationSeconds,
        onConfirmStepEdit = viewModel::confirmStepEdit,
        onCancelStepEdit = viewModel::cancelStepEdit,
    )
}
