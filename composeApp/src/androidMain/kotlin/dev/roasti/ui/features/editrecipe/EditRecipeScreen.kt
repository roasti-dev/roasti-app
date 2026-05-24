package dev.roasti.ui.features.editrecipe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import dev.roasti.R
import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.RoastLevel
import dev.roasti.ui.features.editrecipe.model.EditRecipeEvent
import dev.roasti.ui.features.recipeform.RecipeFormListener
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
    val isDirty by viewModel.isDirty.collectAsStateWithLifecycle()
    var saveErrorTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EditRecipeEvent.SaveSuccess -> onBackClick()
                EditRecipeEvent.SaveError -> saveErrorTick++
                EditRecipeEvent.ImageUploadFailed -> Unit
            }
        }
    }

    when {
        state.isLoading -> LoadingStub()
        state.loadError -> ErrorStub(stringResource(R.string.error_generic))
        else -> RecipeFormScreen(
            form = state.form,
            topBarTitle = stringResource(R.string.edit_recipe_top_bar_title),
            saveButtonLabel = stringResource(R.string.edit_recipe_save_changes),
            isDirty = isDirty,
            isCreateMode = false,
            saveErrorEventTrigger = saveErrorTick,
            listener = EditRecipeFormListener(viewModel, onBackClick),
        )
    }
}

private class EditRecipeFormListener(
    private val viewModel: EditRecipeViewModel,
    private val onBack: () -> Unit,
) : RecipeFormListener {
    override fun onBackClick() = onBack()
    override fun onSaveClick() = viewModel.save()
    override fun onTitleChange(value: String) = viewModel.updateTitle(value)
    override fun onDescriptionChange(value: String) = viewModel.updateDescription(value)
    override fun onBeansChange(value: String) = viewModel.updateBeans(value)
    override fun onBrewMethodChange(value: BrewMethod) = viewModel.updateBrewMethod(value)
    override fun onDifficultyChange(value: Difficulty) = viewModel.updateDifficulty(value)
    override fun onRoastLevelChange(value: RoastLevel) = viewModel.updateRoastLevel(value)
    override fun onUploadImage(fileName: String, bytes: ByteArray) =
        viewModel.uploadImage(fileName, bytes)
    override fun onRemoveImage() = viewModel.removeImage()

    override fun onOpenAddStep() = viewModel.openAddStepEditor()
    override fun onOpenEditStep(index: Int) = viewModel.openEditStepEditor(index)
    override fun onDraftTitleChange(value: String) = viewModel.updateDraftTitle(value)
    override fun onDraftDurationChange(totalSeconds: Int) =
        viewModel.updateDraftDuration(totalSeconds)
    override fun onCommitDraft() = viewModel.commitDraft()
    override fun onCancelDraft() = viewModel.cancelDraft()

    override fun onRemoveStep(index: Int) = viewModel.removeStep(index)
    override fun onReorderSteps(fromIndex: Int, toIndex: Int) =
        viewModel.reorderSteps(fromIndex, toIndex)
}
