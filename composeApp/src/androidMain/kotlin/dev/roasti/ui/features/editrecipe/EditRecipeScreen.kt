package dev.roasti.ui.features.editrecipe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import dev.roasti.R
import dev.roasti.ui.features.editrecipe.model.EditRecipeEvent
import dev.roasti.ui.features.recipeform.RecipeFormScreen
import dev.roasti.ui.uikit.ErrorStub
import dev.roasti.ui.uikit.LoadingStub

@Composable
fun EditRecipeRoute(
    id: String,
    onBackClick: () -> Unit,
) {
    val viewModel: EditRecipeViewModel = koinViewModel(parameters = { parametersOf(id) })
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EditRecipeEvent.SaveSuccess -> onBackClick()
                EditRecipeEvent.SaveError, EditRecipeEvent.ImageUploadFailed -> Unit
            }
        }
    }

    when {
        state.isLoading -> LoadingStub()
        state.loadError -> ErrorStub(stringResource(R.string.error_generic))
        else -> RecipeFormScreen(
            form = state.form,
            saveButtonLabel = stringResource(R.string.edit_recipe_save_changes),
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
}
