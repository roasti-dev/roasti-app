package dev.roasti.ui.features.createrecipe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import dev.roasti.R
import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.RoastLevel
import dev.roasti.ui.features.recipeform.RecipeFormListener
import dev.roasti.ui.features.recipeform.RecipeFormScreen

@Composable
fun CreateRecipeRoute(onBackClick: () -> Unit) {
    val viewModel: CreateRecipeScreenViewModel = koinViewModel()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val isDirty by viewModel.isDirty.collectAsStateWithLifecycle()
    var saveErrorTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                CreateRecipeScreenEvent.SaveSuccess -> onBackClick()
                CreateRecipeScreenEvent.SaveError -> saveErrorTick++
                CreateRecipeScreenEvent.ImageUploadFailed -> Unit
            }
        }
    }

    RecipeFormScreen(
        form = form,
        topBarTitle = stringResource(R.string.create_recipe_top_bar_title),
        saveButtonLabel = stringResource(R.string.create_recipe_create),
        isDirty = isDirty,
        isCreateMode = true,
        saveErrorEventTrigger = saveErrorTick,
        listener = CreateRecipeFormListener(viewModel, onBackClick),
    )
}

private class CreateRecipeFormListener(
    private val viewModel: CreateRecipeScreenViewModel,
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
