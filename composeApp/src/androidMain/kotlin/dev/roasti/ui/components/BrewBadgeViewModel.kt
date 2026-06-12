package dev.roasti.ui.components

import androidx.lifecycle.ViewModel
import dev.roasti.feature.brew.domain.BrewRepository
import dev.roasti.utils.stateInWhileSubscribe
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/** Число активных завариваний для бейджа на табе Recipes. */
internal class BrewBadgeViewModel(
    brewRepository: BrewRepository,
) : ViewModel() {

    val activeCount: StateFlow<Int> =
        brewRepository.observeActive()
            .map { it.size }
            .stateInWhileSubscribe(0)
}
