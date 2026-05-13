package dev.roasti.ui.features.createrecipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dev.roasti.feature.recipe.domain.RecipeRepository
import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.RoastLevel
import dev.roasti.feature.upload.domain.UploadRepository
import dev.roasti.ui.features.recipeform.mapper.toRecipeDraft
import dev.roasti.ui.features.recipeform.model.ActiveStepSheet
import dev.roasti.ui.features.recipeform.model.RecipeFormFields
import dev.roasti.ui.features.recipeform.model.RecipeFormStepUiModel
import dev.roasti.core.utils.imageUrl

class CreateRecipeScreenViewModel(
    private val recipeRepository: RecipeRepository,
    private val uploadRepository: UploadRepository,
) : ViewModel() {

    private val _form = MutableStateFlow(RecipeFormFields())
    val form: StateFlow<RecipeFormFields> = _form.asStateFlow()

    private val _events = MutableSharedFlow<CreateRecipeScreenEvent>()
    val events: SharedFlow<CreateRecipeScreenEvent> = _events.asSharedFlow()

    fun updateTitle(value: String) = _form.update { it.copy(title = value, saveError = false) }
    fun updateDescription(value: String) = _form.update { it.copy(description = value) }
    fun updateBrewMethod(value: BrewMethod) = _form.update { it.copy(brewMethod = value) }
    fun updateDifficulty(value: Difficulty) = _form.update { it.copy(difficulty = value) }
    fun updateRoastLevel(value: RoastLevel) = _form.update { it.copy(roastLevel = value) }
    fun updateBeans(value: String) = _form.update { it.copy(beans = value) }

    fun openAddStep() = _form.update { it.copy(activeStepSheet = ActiveStepSheet(editingIndex = null)) }

    fun openEditStep(index: Int) {
        val step = _form.value.steps.getOrNull(index) ?: return
        _form.update {
            it.copy(
                activeStepSheet = ActiveStepSheet(
                    editingIndex = index,
                    title = step.title,
                    durationMinutes = step.durationSeconds?.let { s -> (s / 60).toString() } ?: "",
                    durationSeconds = step.durationSeconds?.let { s -> (s % 60).toString() } ?: "",
                )
            )
        }
    }

    fun updateActiveStepTitle(value: String) =
        _form.update { it.copy(activeStepSheet = it.activeStepSheet?.copy(title = value)) }

    fun updateActiveStepDurationMinutes(value: String) =
        _form.update { it.copy(activeStepSheet = it.activeStepSheet?.copy(durationMinutes = value)) }

    fun updateActiveStepDurationSeconds(value: String) =
        _form.update { it.copy(activeStepSheet = it.activeStepSheet?.copy(durationSeconds = value)) }

    fun confirmStepEdit() {
        val sheet = _form.value.activeStepSheet ?: return
        if (!sheet.canConfirm) return
        val newStep = RecipeFormStepUiModel(
            order = sheet.editingIndex ?: _form.value.steps.size,
            title = sheet.title,
            durationSeconds = sheet.durationTotalSeconds,
        )
        _form.update {
            val updatedSteps = it.steps.toMutableList()
            val idx = sheet.editingIndex
            if (idx != null) updatedSteps[idx] = newStep else updatedSteps.add(newStep)
            it.copy(steps = updatedSteps, activeStepSheet = null)
        }
    }

    fun cancelStepEdit() = _form.update { it.copy(activeStepSheet = null) }

    fun removeStep(index: Int) = _form.update {
        it.copy(steps = it.steps.toMutableList().also { list -> list.removeAt(index) })
    }

    fun uploadImage(fileName: String, bytes: ByteArray) {
        viewModelScope.launch {
            _form.update { it.copy(isUploadingImage = true) }
            val result = uploadRepository.uploadImage(fileName, bytes)
            if (result.isFailure) _events.emit(CreateRecipeScreenEvent.ImageUploadFailed)
            _form.update { form ->
                form.copy(
                    imageId = result.getOrNull()?.id ?: form.imageId,
                    imageUrl = result.getOrNull()?.id?.let(::imageUrl) ?: form.imageUrl,
                    isUploadingImage = false,
                )
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            _form.update { it.copy(isSaving = true, saveError = false) }
            val result = recipeRepository.addRecipe(_form.value.toRecipeDraft())
            if (result.isSuccess) {
                _events.emit(CreateRecipeScreenEvent.SaveSuccess)
            } else {
                _form.update { it.copy(isSaving = false, saveError = true) }
                _events.emit(CreateRecipeScreenEvent.SaveError)
            }
        }
    }
}
