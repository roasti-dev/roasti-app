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
import dev.roasti.feature.upload.domain.UploadRepository
import dev.roasti.ui.features.createrecipe.mapper.toRecipeDraft
import dev.roasti.ui.features.createrecipe.model.CreateRecipeEvent
import dev.roasti.ui.features.createrecipe.model.CreateRecipeStepUiModel
import dev.roasti.ui.features.createrecipe.model.CreateRecipeUiState
import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.RoastLevel

class CreateRecipeViewModel(
    private val repository: RecipeRepository,
    private val uploadRepository: UploadRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CreateRecipeUiState())
    val state: StateFlow<CreateRecipeUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<CreateRecipeEvent>()

    val events: SharedFlow<CreateRecipeEvent> = _events.asSharedFlow()

    fun updateName(value: String) = _state.update { it.copy(name = value) }
    fun updateBrewMethod(value: BrewMethod?) = _state.update { it.copy(brewMethod = value) }
    fun updateBeans(value: String) = _state.update { it.copy(beans = value) }
    fun updateDifficulty(value: Difficulty) = _state.update { it.copy(difficulty = value) }
    fun updateRoastLevel(value: RoastLevel) = _state.update { it.copy(roastLevel = value) }
    fun updateDescription(value: String) = _state.update { it.copy(description = value) }
    fun addBrewStep(step: CreateRecipeStepUiModel) {
        _state.update { it.copy(brewSteps = it.brewSteps + step, pendingStepImageId = null) }
    }

    fun removeBrewStepByIndex(index: Int) {
        _state.update {
            val updatedList = it.brewSteps.toMutableList()
            updatedList.removeAt(index)
            it.copy(brewSteps = updatedList)
        }
    }

    fun uploadImage(fileName: String, bytes: ByteArray) {
        viewModelScope.launch {
            _state.update { it.copy(isUploadingImage = true) }
            val result = uploadRepository.uploadImage(fileName, bytes)
            if (result.isFailure) _events.emit(CreateRecipeEvent.OnImageUploadFailed)
            _state.update { it.copy(imageId = result.getOrNull()?.id, isUploadingImage = false) }
        }
    }

    fun uploadBrewStepImage(fileName: String, bytes: ByteArray) {
        viewModelScope.launch {
            _state.update { it.copy(isUploadingStepImage = true) }
            val result = uploadRepository.uploadImage(fileName, bytes)
            if (result.isFailure) _events.emit(CreateRecipeEvent.OnImageUploadFailed)
            _state.update { it.copy(pendingStepImageId = result.getOrNull()?.id, isUploadingStepImage = false) }
        }
    }

    fun reset() = _state.update { CreateRecipeUiState() }

    fun publishRecipe() {
        val recipe = state.value.toRecipeDraft()
        viewModelScope.launch {
            val result = repository.addRecipe(recipe)
            _events.emit(CreateRecipeEvent.OnRequestFinished(result.isSuccess))
        }
    }
}
